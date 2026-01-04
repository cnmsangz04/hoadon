<template>
  <div class="container-fluid py-3">
    <b-card class="shadow-sm">
      <div class="d-flex align-items-center justify-content-between mb-2">
        <h4 class="mb-0 font-weight-bold">Lập hóa đơn GTGT</h4>
        <div>
          <b-button size="sm" variant="outline-secondary" @click="goBack">Quay lại danh sách</b-button>
        </div>
      </div>

      <b-alert v-if="error" show variant="danger" class="mb-3">{{ error }}</b-alert>

      <div v-if="isBusy">
        <b-skeleton width="100%" height="20px" animated class="mb-2" />
        <b-skeleton width="96%" height="20px" animated class="mb-2" />
        <b-skeleton width="92%" height="20px" animated class="mb-2" />
      </div>

      <div v-else>
        <b-row>
          <b-col md="6">
            <b-form-group label="Mẫu hóa đơn">
              <b-input-group>
                <b-input-group-prepend is-text>
                  <b-badge variant="primary">{{ prepare.formCode || '—' }}</b-badge>
                </b-input-group-prepend>
                <b-form-input :value="prepare.serial || '—'" readonly />
              </b-input-group>
            </b-form-group>
          </b-col>
          <b-col md="6">
            <b-form-group label="Hình thức">
              <b-form-input :value="haveCodeLabel" readonly />
            </b-form-group>
          </b-col>
        </b-row>

        <b-row>
          <b-col md="6">
            <b-form-group label="Tờ khai được chấp nhận gần nhất">
              <b-form-input :value="prepare.registerId ? ('#' + prepare.registerId) : '—'" readonly />
            </b-form-group>
          </b-col>
          <b-col md="6">
            <b-form-group label="Ngày hiệu lực">
              <b-form-input :value="prepare.registerEffectiveDate || '—'" readonly />
            </b-form-group>
          </b-col>
        </b-row>

        <!-- TODO: invoice creation form fields -->
        <div class="text-right mt-3">
          <b-button variant="success">Lưu và lập hóa đơn</b-button>
        </div>
      </div>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'VatInvoiceCreate',
  data () {
    return {
      isBusy: false,
      error: null,
      prepare: { formId: null, formCode: null, serial: null, haveCode: null, registerId: null, registerEffectiveDate: null },
    }
  },
  computed: {
    haveCodeLabel () {
      const v = this.prepare && this.prepare.haveCode
      return v === 1 ? 'Cấp mã (CMa)' : v === 0 ? 'Không cấp mã (KCMa)' : '—'
    }
  },
  created () { this.checkPrerequisites() },
  methods: {
    async checkPrerequisites () {
      this.isBusy = true
      this.error = null
      try {
        const { data } = await axios.get('/v1/invoices/prepare')
        this.prepare = { ...this.prepare, ...data }
      } catch (e) {
        const msg = e && e.response && e.response.data && e.response.data.message
          ? e.response.data.message
          : 'Không đủ điều kiện để lập hóa đơn'
        this.error = msg
        // Show toast and redirect back to list
        this.$bvToast && this.$bvToast.toast(msg, { title: 'Lỗi', variant: 'danger', solid: true, autoHideDelay: 4000 })
        this.$router.replace({ name: 'CustomerVatInvoiceList' })
      } finally {
        this.isBusy = false
      }
    },
    goBack () { this.$router.push({ name: 'CustomerVatInvoiceList' }) },
  }
}
</script>

<style scoped>
.shadow-sm { border-radius: 10px; }
</style>
