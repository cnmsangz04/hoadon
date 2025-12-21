import Vue from 'vue'
import VueI18n from 'vue-i18n'

Vue.use(VueI18n)

const messages = {
  vi: {
    account: {
      description: 'Quản lý thông tin tài khoản của bạn'
    }
  },
  en: {
    account: {
      description: 'Manage your account information'
    }
  }
}

const i18n = new VueI18n({
  locale: 'vi',          // ngôn ngữ mặc định
  fallbackLocale: 'vi',
  silentTranslationWarn: true,
  messages
})

export default i18n
