/*
 * Nucleus, the software collection powering Xpdustry.
 * Copyright (C) 2022  Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.xpdustry.nucleus.common.translation;

import com.deepl.api.DeepLException;
import com.deepl.api.Language;
import com.deepl.api.LanguageType;
import com.deepl.api.Translator;
import com.deepl.api.TranslatorOptions;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.xpdustry.nucleus.api.application.NucleusListener;
import fr.xpdustry.nucleus.api.application.NucleusRuntime;
import fr.xpdustry.nucleus.api.exception.RatelimitException;
import fr.xpdustry.nucleus.api.translation.TranslationService;
import fr.xpdustry.nucleus.api.translation.UnsupportedLocaleException;
import fr.xpdustry.nucleus.common.configuration.NucleusConfiguration;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

public final class DeeplTranslationService implements TranslationService, NucleusListener {

    private final Translator translator;
    private final AsyncLoadingCache<TranslatorKey, String> cache;

    private final AtomicBoolean rateLimited = new AtomicBoolean(false);
    private final List<Locale> sourceLanguages = new ArrayList<>();
    private final Object sourceLanguagesLock = new Object();
    private final List<Locale> targetLanguages = new ArrayList<>();
    private final Object targetLanguagesLock = new Object();

    @Inject
    public DeeplTranslationService(final NucleusConfiguration configuration, final NucleusRuntime runtime) {
        this.translator = new Translator(
                configuration.getDeeplTranslationToken(), new TranslatorOptions().setTimeout(Duration.ofSeconds(3L)));
        this.cache = Caffeine.newBuilder()
                .executor(runtime.getAsyncExecutor())
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .buildAsync(this::translate0);
    }

    @Override
    public void onNucleusInit() {
        // Fetch languages
        this.getLanguages(LanguageType.Source);
        this.getLanguages(LanguageType.Target);
        this.updateRateLimit();
    }

    @Override
    public CompletableFuture<String> translate(final String text, final Locale source, final Locale target) {
        if (source.getLanguage().equals("router") || target.getLanguage().equals("router")) {
            return CompletableFuture.completedFuture("router");
        }
        var sourceLocale = findClosestLanguage(LanguageType.Source, source);
        if (sourceLocale.isEmpty()) {
            return CompletableFuture.failedFuture(new UnsupportedLocaleException(source));
        }
        var targetLocale = findClosestLanguage(LanguageType.Target, target);
        if (targetLocale.isEmpty()) {
            return CompletableFuture.failedFuture(new UnsupportedLocaleException(target));
        }
        if (text.isBlank()
                || sourceLocale.get().getLanguage().equals(targetLocale.get().getLanguage())) {
            return CompletableFuture.completedFuture(text);
        }
        if (this.rateLimited.get()) {
            return CompletableFuture.failedFuture(new RatelimitException());
        }
        return this.cache.get(new TranslatorKey(text, sourceLocale.get(), targetLocale.get()));
    }

    private String translate0(final TranslatorKey key) throws Exception {
        final var result = this.translator
                .translateText(key.text, key.source.getLanguage(), key.target.toLanguageTag())
                .getText();
        this.updateRateLimit();
        return result;
    }

    @Override
    public List<Locale> getSupportedLanguages() {
        return getLanguages(LanguageType.Source);
    }

    @Override
    public boolean isSupportedLanguage(final Locale locale) {
        return findClosestLanguage(LanguageType.Source, locale).isPresent();
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private List<Locale> getLanguages(final LanguageType type) {
        final var languages =
                switch (type) {
                    case Source -> sourceLanguages;
                    case Target -> targetLanguages;
                };
        final var lock =
                switch (type) {
                    case Source -> sourceLanguagesLock;
                    case Target -> targetLanguagesLock;
                };
        try {
            synchronized (lock) {
                if (languages.isEmpty()) {
                    languages.addAll(translator.getLanguages(type).stream()
                            .map(Language::getCode)
                            .map(Locale::forLanguageTag)
                            .toList());
                }
            }
            return Collections.unmodifiableList(languages);
        } catch (final DeepLException | InterruptedException e) {
            throw new RuntimeException(
                    "Failed to fetch the %s languages.".formatted(type.name().toLowerCase(Locale.ROOT)), e);
        }
    }

    private Optional<Locale> findClosestLanguage(final LanguageType type, final Locale locale) {
        final var candidates = getLanguages(type).stream()
                .filter(language -> locale.getLanguage().equals(language.getLanguage()))
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        } else if (candidates.size() == 1) {
            return Optional.of(candidates.get(0));
        } else {
            return candidates.stream()
                    .filter(language -> locale.getCountry().equals(language.getCountry()))
                    .findFirst()
                    .or(() -> Optional.of(candidates.get(0)));
        }
    }

    private void updateRateLimit() {
        try {
            final var usage = this.translator.getUsage().getCharacter();
            if (usage == null) {
                throw new RuntimeException("Failed to fetch the character usage.");
            }
            this.rateLimited.set(usage.limitReached());
        } catch (final DeepLException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch the character usage.", e);
        }
    }

    private record TranslatorKey(String text, Locale source, Locale target) {}
}
