<template>
  <div class="container-fluid py-3 email-templates">

    <!-- Ti�u d? v� thao t�c -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Danh sách mẫu email</h4>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button 
          size="sm" 
          variant="success"
          :to="{ name: 'admin-email-template-create' }"
        >
          <i class="fas fa-plus"></i>
          Thêm mẫu email
        </b-button>
      </div>
    </div>

    <!-- H�ng b? l?c -->
    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="4" class="mb-2">
          <b-form-group label="Công ty" label-class="font-weight-500">
            <b-input-group>
              <b-input-group-prepend is-text>
                <i class="fas fa-building text-muted"></i>
              </b-input-group-prepend>
              <b-form-input
                :value="company ? company.name : ''"
                readonly
                disabled
              />
            </b-input-group>
          </b-form-group>
        </b-col>
        <b-col md="4" class="mb-2">
          <b-form-group label="Trạng thái" label-class="font-weight-500">
            <b-form-select v-model="filter.status" :options="statusOptions">
              <template #first>
                <b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option>
              </template>
            </b-form-select>
          </b-form-group>
        </b-col>
        <b-col md="4" class="mb-2 d-flex align-items-end">
          <b-button size="sm" variant="primary" @click="fetchData" class="mb-3">
            <i class="fas fa-search"></i> Tìm kiếm
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <!-- B?ng -->
    <b-card class="shadow-sm">
      <b-table
        bordered
        hover
        responsive
        small
        show-empty
        :items="filteredItems"
        :fields="fields"
        empty-text="Không có dữ liệu"
        class="mb-0 table-modern table-compact"
      >
        <!-- STT -->
        <template #cell(index)="row">
          {{ row.index + 1 }}
        </template>

        <!-- C�ng ty -->
        <template #cell(companyName)="row">
          {{ row.item.companyName || '-' }}
        </template>

        <!-- Kh�a -->
        <template #cell(key)="row">
          <div>
            <div class="text-mono font-weight-bold">{{ row.item.key }}</div>
            <div v-if="row.item.system === 0" class="mt-1">
              <b-badge variant="danger">Hệ thống</b-badge>
            </div>
          </div>
        </template>

        <!-- Ti�u d? -->
        <template #cell(title)="row">
          <div class="font-weight-bold">{{ row.item.title }}</div>
        </template>

        <!-- Tr?ng th�i -->
        <template #cell(status)="row">
          <b-badge :variant="row.item.status === 1 ? 'success' : 'secondary'">
            {{ row.item.status === 1 ? 'Kích hoạt' : 'Ngưng hoạt động' }}
          </b-badge>
        </template>

        <!-- C?p nh?t l�c -->
        <template #cell(updatedAt)="row">
          {{ formatDate(row.item.updatedAt) }}
        </template>

        <!-- H�nh d?ng -->
        <template #cell(actions)="row">
          <b-dropdown
            size="sm"
            right
            variant="link"
            toggle-class="text-decoration-none"
            no-caret
            boundary="window"
          >
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" @click="edit(row.item.id)">
              <i class="fas fa-edit"></i> Cập nhật
            </b-dropdown-item>
            <b-dropdown-divider></b-dropdown-divider>
            <b-dropdown-item class="text-center text-danger" @click="remove(row.item.id)">
              <i class="fas fa-trash"></i> Xóa
            </b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>
    </b-card>

  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'MailTemplateList',

  data() {
    return {
      company: null,

      filter: {
        status: null
      },

      statusOptions: [
        { value: 1, text: 'Kích hoạt' },
        { value: 0, text: 'Ngưng hoạt động' }
      ],

      fields: [
        { key: 'index', label: '#', thStyle: { width: '50px' }, class: 'text-center' },
        { key: 'companyName', label: 'Công ty' },
        { key: 'key', label: 'Mã template' },
        { key: 'title', label: 'Tiêu đề' },
        { key: 'status', label: 'Trạng thái', class: 'text-center' },
        { key: 'updatedAt', label: 'Cập nhật', class: 'text-center' },
        { key: 'actions', label: 'Thao tác', thStyle: { width: '80px' }, class: 'text-center' }
      ],

      items: []
    }
  },

  computed: {
    filteredItems() {
      if (this.filter.status === null) {
        return this.items;
      }
      return this.items.filter(item => item.status === this.filter.status);
    }
  },

  created() {
    this.initCompany()
  },

  methods: {
    initCompany() {
      const timer = setInterval(() => {
        const company = this.$app?.info?.company

        if (company && company.id) {
          clearInterval(timer)
          this.company = company
          this.fetchData()
        }
      }, 50)
    },

    fetchData() {
      axios.get('/administrator/mail-template', {
        params: {
          companyId: this.company.id
        }
      }).then(res => {
        this.items = res.data || []
      }).catch(err => {
        const message = err.response?.data?.message || 'Không thể tải danh sách mẫu email';
        this.$toastr && this.$toastr.error(message);
      })
    },

    edit(id) {
      this.$router.push({
        name: 'admin-email-template-create',
        params: { id }
      })
    },

    remove(id) {
      if (!confirm('Bạn có chắc muốn xóa mẫu email này?')) {
        return;
      }

      axios.delete(`/administrator/mail-template/${id}`)
        .then(() => {
          this.$toastr && this.$toastr.success('Xóa thành công')
          this.fetchData()
        })
        .catch(err => {
          const message = err.response?.data?.message || 'Không thể xóa mẫu email';
          this.$toastr && this.$toastr.error(message);
        })
    },

    formatDate(date) {
      if (!date) return '-';
      const d = new Date(date);
      const day = String(d.getDate()).padStart(2, '0');
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const year = d.getFullYear();
      return `${day}/${month}/${year}`;
    },

    reload() {
      this.filter.status = null;
      this.fetchData();
    }
  }
}
</script>


<style scoped>
.email-templates .card.shadow-sm { 
  border-radius: 10px; 
}

.email-templates .table-hover tbody tr:hover { 
  background-color: #fafbfd; 
}

.email-templates .btn-outline-primary { 
  border-color: #dfe7ff; 
}

.email-templates .btn-outline-primary:hover { 
  background: #eef3ff; 
}

.email-templates .table thead th { 
  background: #f7f9fc; 
  border-bottom: 1px solid #ecf0f6; 
  color: #4a5568; 
  font-weight: 700; 
}

/* Giữ tinh chỉnh bảng hiện đại hiện có */
.table-modern thead th { 
  background-color: #f9fafb; 
  border-bottom: 2px solid #e5e7eb; 
  position: sticky; 
  top: 0; 
  z-index: 1; 
}

.table-compact td, .table-compact th { 
  padding: 0.5rem 0.75rem; 
}

.table td { 
  vertical-align: middle; 
}

.table-modern tbody tr:hover { 
  background-color: #f6f8fa; 
}

.table-modern.table-striped tbody tr:nth-of-type(odd) { 
  background-color: #fcfdff; 
}

.table-modern { 
  border-color: #e9ecef; 
}

.text-mono { 
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; 
}

.badge { 
  font-size: 13px; 
}

.card { 
  border: 1px solid #e9ecef; 
}

.card-body { 
  padding: 0.75rem 1rem; 
}

.font-weight-500 {
  font-weight: 500;
  color: #4a5568;
  font-size: 0.9rem;
}

.input-group-text {
  background: #f7fafc;
  border-right: none;
  color: #718096;
  min-width: 40px;
  justify-content: center;
}

.input-group .form-control {
  border-left: none;
}

.input-group .form-control:focus {
  border-color: #cbd5e0;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.1);
}

.input-group:focus-within .input-group-text {
  border-color: #cbd5e0;
  background: #edf2f7;
}
</style>
