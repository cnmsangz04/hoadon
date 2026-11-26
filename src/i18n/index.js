import Vue from 'vue'
import VueI18n from 'vue-i18n'
import en from './locales/en'
import vi from './locales/vi'
import { LANGUAGE_STORAGE_KEY, languageInfo, languages, normalizeLanguage } from './languages'

Vue.use(VueI18n)

function storedLanguage() {
  try {
    return localStorage.getItem(LANGUAGE_STORAGE_KEY)
  } catch {
    return null
  }
}

export const defaultLanguage = normalizeLanguage(storedLanguage())

const i18n = new VueI18n({
  locale: defaultLanguage,
  fallbackLocale: 'vi',
  silentTranslationWarn: true,
  messages: { vi, en },
})

export function setLanguage(code) {
  const locale = normalizeLanguage(code)
  i18n.locale = locale
  const info = languageInfo(locale)
  document.documentElement.setAttribute('lang', info.htmlLang)
  try {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, locale)
  } catch {}
  window.dispatchEvent(new CustomEvent('app-language-changed', { detail: { locale } }))
  return locale
}

export function currentIntlLocale() {
  return i18n.locale === 'en' ? 'en-US' : 'vi-VN'
}

export { languages }

setLanguage(defaultLanguage)

export default i18n
