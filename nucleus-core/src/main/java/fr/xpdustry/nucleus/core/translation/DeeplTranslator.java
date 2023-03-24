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
package fr.xpdustry.nucleus.core.translation;

import com.deepl.api.DeepLException;
import com.deepl.api.Language;
import com.deepl.api.LanguageType;
import com.deepl.api.TranslatorOptions;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class DeeplTranslator implements Translator {

    private final com.deepl.api.Translator translator;
    private final Executor executor;
    private final AsyncLoadingCache<TranslatorKey, String> cache;

    private final List<Locale> sourceLanguages = new ArrayList<>();
    private final Object sourceLanguagesLock = new Object();
    private final List<Locale> targetLanguages = new ArrayList<>();
    private final Object targetLanguagesLock = new Object();

    public DeeplTranslator(final String key, final Executor executor) {
        this.translator = new com.deepl.api.Translator(key, new TranslatorOptions().setTimeout(Duration.ofSeconds(3L)));
        this.executor = executor;
        this.cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .buildAsync(this::translate0);
    }

    @Override
    public CompletableFuture<String> translate(final String text, final Locale source, final Locale target) {
        if (source.getLanguage().equals("router") || target.getLanguage().equals("router")) {
            return CompletableFuture.completedFuture("router");
        }
        return this.cache.get(new TranslatorKey(text, source, target));
    }

    private String translate0(final TranslatorKey key) throws Exception {
        var sourceLocale = findClosestLanguage(LanguageType.Source, key.source)
                .orElseThrow(() -> new UnsupportedLocaleException(key.source));
        var targetLocale = findClosestLanguage(LanguageType.Target, key.target)
                .orElseThrow(() -> new UnsupportedLocaleException(key.target));
        if (sourceLocale.getLanguage().equals(targetLocale.getLanguage())) {
            return key.text;
        }
        return this.translator
                .translateText(key.text, sourceLocale.getLanguage(), targetLocale.toLanguageTag())
                .getText();
    }

    @Override
    public CompletableFuture<List<Locale>> getSupportedLanguages() {
        return CompletableFuture.supplyAsync(() -> getLanguages(LanguageType.Source), executor);
    }

    @Override
    public CompletableFuture<Boolean> isSupportedLanguage(final Locale locale) {
        return CompletableFuture.supplyAsync(
                () -> findClosestLanguage(LanguageType.Source, locale).isPresent(), executor);
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
                    "Failed to get the %s languages.".formatted(type.name().toLowerCase(Locale.ROOT)), e);
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

    private record TranslatorKey(String text, Locale source, Locale target) {}
}
