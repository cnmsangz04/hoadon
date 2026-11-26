<template>
  <div class="language-switcher" :class="{ compact, open }">
    <i class="fas fa-globe" aria-hidden="true"></i>
    <span v-if="!compact" class="language-label">{{ $t('language.label') }}</span>
    <button
      type="button"
      class="language-toggle"
      :aria-label="$t('language.label')"
      :aria-expanded="open ? 'true' : 'false'"
      @click="toggle"
    >
      <span>{{ compact ? currentLanguage.shortLabel : currentLanguage.label }}</span>
      <i class="fas fa-chevron-down" aria-hidden="true"></i>
    </button>

    <div v-if="open" class="language-menu" role="menu">
      <button
        v-for="language in languageOptions"
        :key="language.code"
        type="button"
        class="language-option"
        :class="{ active: language.code === selectedLanguage }"
        role="menuitem"
        @click="choose(language.code)"
      >
        <span>{{ language.label }}</span>
        <i v-if="language.code === selectedLanguage" class="fas fa-check" aria-hidden="true"></i>
      </button>
    </div>
  </div>
</template>

<script>
import { languages, setLanguage } from '@/i18n'

export default {
  name: 'LanguageSwitcher',
  props: {
    compact: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      languageOptions: languages,
      open: false,
    }
  },
  computed: {
    currentLanguage() {
      return this.languageOptions.find(language => language.code === this.$i18n.locale) || this.languageOptions[0]
    },
    selectedLanguage: {
      get() {
        return this.$i18n.locale
      },
      set(value) {
        const locale = setLanguage(value)
        this.$emit('change', locale)
      },
    },
  },
  mounted() {
    document.addEventListener('click', this.handleOutside)
  },
  beforeDestroy() {
    document.removeEventListener('click', this.handleOutside)
  },
  methods: {
    toggle() {
      this.open = !this.open
    },
    choose(value) {
      const locale = setLanguage(value)
      this.$emit('change', locale)
      this.open = false
    },
    handleOutside(event) {
      if (!this.$el.contains(event.target)) this.open = false
    },
  },
}
</script>

<style scoped>
.language-switcher {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 36px;
  margin: 0;
  padding: 0 8px;
  border: 1px solid #dce3ec;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.96);
  color: #334155;
  font-size: 13px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.language-switcher:hover,
.language-switcher.open {
  border-color: #9bb8ff;
  box-shadow: 0 5px 16px rgba(37, 99, 235, 0.14);
}

.language-switcher i {
  color: #4f77ff;
}

.language-label {
  color: #475569;
  font-weight: 600;
  white-space: nowrap;
}

.language-toggle {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-width: 96px;
  height: 30px;
  padding: 0 5px;
  border: 0;
  outline: 0;
  background: transparent;
  color: #1e293b;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.language-toggle i {
  margin-left: auto;
  color: #64748b;
  font-size: 10px;
  transition: transform 0.16s ease;
}

.language-switcher.open .language-toggle i {
  transform: rotate(180deg);
}

.language-menu {
  position: absolute;
  z-index: 1100;
  top: calc(100% + 7px);
  right: 0;
  min-width: 148px;
  padding: 5px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.18);
  animation: language-menu-in 0.14s ease-out;
}

.language-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 32px;
  padding: 0 9px;
  border: 0;
  outline: 0;
  border-radius: 7px;
  background: transparent;
  color: #334155;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.language-option:hover,
.language-option.active {
  background: #edf3ff;
  color: #2456c6;
  font-weight: 600;
}

.language-option i {
  color: #3b66f0;
  font-size: 11px;
}

.language-switcher.compact {
  min-width: 68px;
  justify-content: center;
  padding: 0 6px;
}

.language-switcher.compact .language-toggle {
  min-width: 39px;
  width: 39px;
  justify-content: center;
  padding: 0;
}

.language-switcher.compact .language-toggle i {
  margin-left: 2px;
}

@keyframes language-menu-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
