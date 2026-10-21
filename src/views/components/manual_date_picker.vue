<template>
  <b-input-group class="manual-date-picker" :class="groupClass">
    <b-form-input
      :id="id"
      v-model="manualValue"
      type="text"
      :placeholder="placeholder"
      autocomplete="off"
      :size="size"
      :disabled="disabled"
      :state="state"
      @input="onManualInput"
    />
    <b-input-group-append>
      <b-form-datepicker
        v-model="pickerValue"
        locale="vi"
        :size="size || 'sm'"
        button-only
        :right="right"
        :disabled="disabled"
        :min="min || undefined"
        :max="max || undefined"
        menu-class="manual-date-picker-menu"
        :date-format-options="dateFormatOptions"
      />
    </b-input-group-append>
  </b-input-group>
</template>

<script>
export default {
  name: 'ManualDatePicker',
  props: {
    value: { type: [String, Date], default: null },
    id: { type: String, default: null },
    placeholder: { type: String, default: 'dd/mm/yyyy' },
    size: { type: String, default: 'sm' },
    min: { type: [String, Date], default: null },
    max: { type: [String, Date], default: null },
    disabled: { type: Boolean, default: false },
    right: { type: Boolean, default: true },
    state: { type: Boolean, default: null },
    groupClass: { type: [String, Array, Object], default: 'mb-3' }
  },
  data() {
    return {
      manualValue: this.formatDate(this.value)
    }
  },
  computed: {
    pickerValue: {
      get() {
        return this.toIsoDate(this.value)
      },
      set(value) {
        this.onPickerInput(value)
      }
    },
    dateFormatOptions() {
      return { year: 'numeric', month: '2-digit', day: '2-digit' }
    }
  },
  watch: {
    value(value) {
      this.manualValue = this.formatDate(value)
    }
  },
  methods: {
    toIsoDate(value) {
      if (!value) return null
      if (value instanceof Date) {
        if (Number.isNaN(value.getTime())) return null
        return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`
      }
      const text = String(value).trim()
      const iso = text.match(/^(\d{4})-(\d{2})-(\d{2})/)
      return iso ? `${iso[1]}-${iso[2]}-${iso[3]}` : this.parseDate(text)
    },
    formatDate(value) {
      const iso = this.toIsoDate(value)
      if (!iso) return ''
      const match = iso.match(/^(\d{4})-(\d{2})-(\d{2})$/)
      return match ? `${match[3]}/${match[2]}/${match[1]}` : ''
    },
    parseDate(value) {
      const match = String(value || '').trim().match(/^(\d{1,2})[/\.\-](\d{1,2})[/\.\-](\d{4})$/)
      if (!match) return null
      const day = match[1].padStart(2, '0')
      const month = match[2].padStart(2, '0')
      const year = match[3]
      const date = new Date(Number(year), Number(month) - 1, Number(day))
      if (date.getFullYear() !== Number(year) || date.getMonth() !== Number(month) - 1 || date.getDate() !== Number(day)) return null
      return `${year}-${month}-${day}`
    },
    onManualInput(value) {
      this.manualValue = value
      const text = String(value || '').trim()
      const parsed = this.parseDate(text)
      if (parsed || !text) this.$emit('input', parsed)
    },
    onPickerInput(value) {
      const iso = this.toIsoDate(value)
      this.manualValue = this.formatDate(iso)
      this.$emit('input', iso)
    }
  }
}
</script>
