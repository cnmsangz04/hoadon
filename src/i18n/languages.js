export const LANGUAGE_STORAGE_KEY = 'app-language'

export const languages = [
  { code: 'vi', label: 'Tiếng Việt', shortLabel: 'VI', htmlLang: 'vi' },
  { code: 'en', label: 'English', shortLabel: 'EN', htmlLang: 'en' },
]

export function isSupportedLanguage(code) {
  return languages.some(language => language.code === code)
}

export function normalizeLanguage(code) {
  const value = String(code || '').trim().toLowerCase().split('-')[0]
  return isSupportedLanguage(value) ? value : 'vi'
}

export function languageInfo(code) {
  return languages.find(language => language.code === normalizeLanguage(code)) || languages[0]
}
