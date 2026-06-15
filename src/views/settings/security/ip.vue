<template>
  <div class="container-fluid py-3 ip-security">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Bảo mật bằng IP</h4>
      <b-button size="sm" variant="outline-primary" @click="reload">
        <i class="fas fa-sync-alt mr-1"></i>
        Làm mới
      </b-button>
    </div>

    <b-card class="mb-3 shadow-sm">
      <b-row class="align-items-center">
        <b-col md="8" class="mb-3 mb-md-0">
          <div class="security-status">
            <div class="status-icon" :class="{ active: enabled }">
              <i class="fas" :class="enabled ? 'fa-shield-alt' : 'fa-shield-virus'"></i>
            </div>
            <div>
              <div class="font-weight-bold">
                Trạng thái:
                <b-badge :variant="enabled ? 'success' : 'secondary'">
                  {{ enabled ? 'Kích hoạt' : 'Chưa kích hoạt' }}
                </b-badge>
              </div>
              <div class="text-muted small mt-1">
                IP hiện tại: <code>{{ currentIp || '—' }}</code>
              </div>
            </div>
          </div>
        </b-col>
        <b-col md="4" class="text-md-right">
          <b-form-checkbox
            v-model="enabled"
            switch
            size="lg"
            :disabled="savingStatus || loading"
            @change="changeEnabled"
          >
            {{ enabled ? 'Kích hoạt' : 'Chưa kích hoạt' }}
          </b-form-checkbox>
        </b-col>
      </b-row>
    </b-card>

    <b-card class="mb-3 shadow-sm">
      <b-form @submit.prevent="addIp">
        <b-row>
          <b-col md="4" class="mb-2">
            <b-form-input v-model.trim="form.ipAddress" placeholder="Nhập IP được phép" />
          </b-col>
          <b-col md="5" class="mb-2">
            <b-form-input v-model.trim="form.note" placeholder="Ghi chú" />
          </b-col>
          <b-col md="3" class="mb-2 text-md-right">
            <b-button size="sm" variant="outline-secondary" class="mr-2" type="button" @click="useCurrentIp">
              IP hiện tại
            </b-button>
            <b-button size="sm" variant="primary" type="submit" :disabled="savingIp">
              <i class="fas fa-plus mr-1"></i>
              Thêm IP
            </b-button>
          </b-col>
        </b-row>
      </b-form>
    </b-card>

    <b-card class="shadow-sm">
      <b-table
        bordered
        hover
        responsive
        small
        show-empty
        class="mb-0 table-modern table-compact"
        :busy="loading"
        :items="pagedIps"
        :fields="fields"
        empty-text="Chưa có IP nào được phép"
      >
        <template #cell(index)="{ index }">
          {{ index + 1 + (list.current_page - 1) * list.per_page }}
        </template>

        <template #cell(ipAddress)="{ item }">
          <code>{{ item.ipAddress }}</code>
        </template>

        <template #cell(type)="{ item }">
          <b-badge :variant="item.originalIp ? 'primary' : 'light'">
            {{ item.originalIp ? 'IP gốc' : 'Bổ sung' }}
          </b-badge>
        </template>

        <template #cell(status)="{ item }">
          <b-badge :variant="Number(item.status) === 1 ? 'success' : 'secondary'">
            {{ Number(item.status) === 1 ? 'Kích hoạt' : 'Chưa kích hoạt' }}
          </b-badge>
        </template>

        <template #cell(createdAt)="{ item }">
          {{ formatDateTime(item.createdAt) }}
        </template>

        <template #cell(option)="{ item }">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item
              v-if="!item.originalIp"
              href="#"
              class="text-center text-danger"
              @click.prevent="deleteIp(item)"
            >
              Xóa
            </b-dropdown-item>
            <b-dropdown-item v-else href="#" class="text-center text-muted" disabled>
              Không thể xóa
            </b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <pagination-bar
        :current.sync="list.current_page"
        :size.sync="list.per_page"
        :total="ips.length"
        :sizes="pageSizes"
        @page-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'

export default {
  name: 'SettingIpSecurity',
  components: { PaginationBar },
  data() {
    return {
      loading: false,
      savingStatus: false,
      savingIp: false,
      enabled: false,
      currentIp: '',
      ips: [],
      form: {
        ipAddress: '',
        note: ''
      },
      pageSizes: [10, 20, 50, 100],
      list: {
        current_page: 1,
        per_page: 10
      },
      fields: [
        { key: 'index', label: '#', thStyle: { width: '60px' }, tdClass: 'text-center' },
        { key: 'ipAddress', label: 'IP', thStyle: { width: '180px' } },
        { key: 'type', label: 'Loại', thStyle: { width: '120px' }, tdClass: 'text-center' },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '130px' }, tdClass: 'text-center' },
        { key: 'note', label: 'Ghi chú' },
        { key: 'createdAt', label: 'Ngày thêm', thStyle: { width: '170px' } },
        { key: 'option', label: '', thStyle: { width: '100px' }, tdClass: 'text-center' }
      ]
    }
  },
  computed: {
    pagedIps() {
      const start = (this.list.current_page - 1) * this.list.per_page
      return this.ips.slice(start, start + this.list.per_page)
    }
  },
  mounted() {
    this.reload()
  },
  methods: {
    async reload() {
      this.loading = true
      try {
        const { data } = await axios.get('/setting/security/ip')
        this.applyState(data)
      } catch (e) {
        const message = e?.response?.data?.message || 'Không thể tải cấu hình bảo mật IP'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.loading = false
      }
    },
    applyState(data) {
      this.enabled = !!data.enabled
      this.currentIp = data.currentIp || ''
      this.ips = Array.isArray(data.ips) ? data.ips : []
      const maxPage = Math.max(1, Math.ceil(this.ips.length / this.list.per_page))
      if (this.list.current_page > maxPage) this.list.current_page = maxPage
    },
    async changeEnabled(value) {
      const previous = !value
      this.savingStatus = true
      try {
        const { data } = await axios.post('/setting/security/ip/status', { enabled: !!value })
        this.applyState(data)
        this.$toastr && this.$toastr.success(this.enabled ? 'Đã kích hoạt bảo mật IP' : 'Đã tắt bảo mật IP')
      } catch (e) {
        this.enabled = previous
        const message = e?.response?.data?.message || 'Không thể cập nhật trạng thái bảo mật IP'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.savingStatus = false
      }
    },
    useCurrentIp() {
      this.form.ipAddress = this.currentIp || ''
      if (!this.form.note) this.form.note = 'IP hiện tại'
    },
    async addIp() {
      if (!this.form.ipAddress) {
        this.$toastr && this.$toastr.warning('Vui lòng nhập IP')
        return
      }
      this.savingIp = true
      try {
        const { data } = await axios.post('/setting/security/ip/allow', {
          ipAddress: this.form.ipAddress,
          note: this.form.note
        })
        this.applyState(data)
        this.form = { ipAddress: '', note: '' }
        this.$toastr && this.$toastr.success('Đã thêm IP được phép')
      } catch (e) {
        const message = e?.response?.data?.message || 'Không thể thêm IP'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.savingIp = false
      }
    },
    async deleteIp(item) {
      const ok = await this.$bvModal.msgBoxConfirm(`Xóa IP ${item.ipAddress}?`, {
        title: 'Xóa IP được phép',
        okTitle: 'Xóa',
        cancelTitle: 'Hủy',
        okVariant: 'danger',
        centered: true
      })
      if (!ok) return
      try {
        const { data } = await axios.delete(`/setting/security/ip/allow/${item.id}`)
        this.applyState(data)
        this.$toastr && this.$toastr.success('Đã xóa IP')
      } catch (e) {
        const message = e?.response?.data?.message || 'Không thể xóa IP'
        this.$toastr && this.$toastr.error(message)
      }
    },
    onPageChange(page) {
      this.list.current_page = Number(page) || 1
    },
    onPageSizeChange(size) {
      this.list.per_page = Number(size) || this.list.per_page
      this.list.current_page = 1
    },
    formatDateTime(value) {
      if (!value) return '—'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
      const dd = String(date.getDate()).padStart(2, '0')
      const mm = String(date.getMonth() + 1).padStart(2, '0')
      const yyyy = date.getFullYear()
      const hh = String(date.getHours())
      const mi = String(date.getMinutes()).padStart(2, '0')
      const ss = String(date.getSeconds()).padStart(2, '0')
      return `${dd}-${mm}-${yyyy} ${hh}:${mi}:${ss}`
    }
  }
}
</script>

<style scoped>
.ip-security .card {
  border-radius: 10px;
}

.security-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-icon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  background: #f1f5f9;
  font-size: 20px;
}

.status-icon.active {
  color: #15803d;
  background: #dcfce7;
}

.ip-security .table th {
  background: #f7f9fc;
  color: #4a5568;
  vertical-align: middle;
}

.ip-security .table td {
  vertical-align: middle;
}
</style>
