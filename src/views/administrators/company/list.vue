<template>
  <div class="container-fluid py-3 permissions">

    <!-- Ti�u d? v� thao t�c -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Danh sách công ty</h4>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="onFilter">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button v-if="canCreateCompany" size="sm" variant="success" @click="showModal()">
          <i class="fas fa-plus"></i>
          Thêm công ty
        </b-button>
      </div>
    </div>

    <!-- H�ng b? l?c -->
    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="4" class="mb-2">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input
              v-model.trim="filter.keyword"
              placeholder="Tìm theo MST / Tên / Địa chỉ"
              @keyup.enter="onFilter"
            />
          </b-input-group>
        </b-col>
        <b-col md="4" class="mb-2">
          <b-form-select v-model="filter.status" :options="statusOptions">
            <template #first>
              <b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option>
            </template>
          </b-form-select>
        </b-col>
        <b-col md="4" class="text-right mb-2">
          <b-button size="sm" variant="primary" @click="onFilter">Tìm kiếm</b-button>
        </b-col>
      </b-row>
    </b-card>

    <!-- B?ng -->
    <b-card class="shadow-sm">
      <b-table
        ref="tblCompany"
        bordered
        hover
        responsive
        small
        show-empty
        :fields="fields"
        :items="dataProvider"
        :busy.sync="isBusy"
        :per-page="list.per_page"
        :current-page="list.current_page"
        empty-text="Không có dữ liệu"
        class="mb-0 table-modern table-compact"
      >
        <!-- STT -->
        <template #cell(id)="data">
          {{ safeRowNumber(data.index) }}
        </template>

        <template #cell(company)="data">
          <div class="font-weight-bold">{{ data.item.name || data.item.companyName }} - {{ data.item.taxcode }}</div>
          <div class="text-muted small">{{ data.item.address || data.item.companyAddress }}</div>
        </template>

        <template #cell(email)="data">
          <div>{{ data.item.email }}</div>
          <div class="text-muted small" v-if="data.item.hotline">Hotline: {{ data.item.hotline }}</div>
        </template>

        <template #cell(status)="data">
          <b-badge :variant="isActive(data.item) ? 'success' : 'secondary'">
            {{ isActive(data.item) ? 'Kích hoạt' : 'Ngưng hoạt động' }}
          </b-badge>
        </template>

        <template #cell(option)="data">
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
            <b-dropdown-item v-if="canUpdateCompany" class="text-center" href="#" @click.prevent="editCompany(data.item)">Cập nhật</b-dropdown-item>
            <b-dropdown-item v-if="canUpdateCompany" class="text-center" href="#" @click.prevent="sendAdminInfo(data.item)">Gửi thông tin</b-dropdown-item>
            <b-dropdown-item v-if="canUpdateCompany" class="text-center text-success" href="#" @click.prevent="setCompanyStatus(data.item.id, 1)">Kích hoạt</b-dropdown-item>
            <b-dropdown-item v-if="canUpdateCompany" class="text-center text-warning" href="#" @click.prevent="setCompanyStatus(data.item.id, 0)">Tạm ngưng</b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <b-pagination
        v-if="list.total > list.per_page"
        v-model="list.current_page"
        :per-page="list.per_page"
        :total-rows="list.total"
        align="right"
        class="mt-2"
        @change="onPageChange"
      />
    </b-card>

    <!-- Modal Thêm/Cập nhật -->
    <b-modal 
      id="companyModal" 
      ref="companyModal" 
      :title="companyForm.id ? 'Cập nhật công ty' : 'Thêm công ty mới'"
      hide-footer
      size="lg"
      modal-class="company-modal"
      header-class="company-modal-header"
      body-class="company-modal-body"
    >
      <b-form @submit.prevent="saveCompany" class="company-form">
        
        <!-- Thông tin cơ bản -->
        <div class="form-section">
          <div class="section-header">
            <i class="fas fa-building text-primary"></i>
            <h6 class="mb-0">Thông tin công ty</h6>
          </div>
          <b-row>
            <b-col cols="12">
              <b-form-group label="Tên công ty" label-for="name" class="required-field">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-building"></i>
                  </b-input-group-prepend>
                  <b-form-input 
                    id="name" 
                    v-model="companyForm.name" 
                    placeholder="Nhập tên công ty"
                    required 
                  />
                </b-input-group>
              </b-form-group>
            </b-col>
            
            <b-col cols="12" md="6">
              <b-form-group label="Mã số thuế" label-for="taxcode" class="required-field">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-hashtag"></i>
                  </b-input-group-prepend>
                  <b-form-input 
                    id="taxcode" 
                    v-model.trim="companyForm.taxcode"
                    placeholder="Nhập mã số thuế"
                    required 
                  />
                </b-input-group>
              </b-form-group>
            </b-col>
            
            <b-col cols="12" md="6">
              <b-form-group label="Trạng thái" label-for="status">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-toggle-on"></i>
                  </b-input-group-prepend>
                  <b-form-select 
                    id="status" 
                    v-model="companyForm.status" 
                    :options="statusOptions" 
                  />
                </b-input-group>
              </b-form-group>
            </b-col>
            
            <b-col cols="12">
              <b-form-group label="Địa chỉ" label-for="address">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-map-marker-alt"></i>
                  </b-input-group-prepend>
                  <b-form-input 
                    id="address" 
                    v-model="companyForm.address"
                    placeholder="Nhập địa chỉ công ty"
                  />
                </b-input-group>
              </b-form-group>
            </b-col>
          </b-row>
        </div>

        <!-- Thông tin liên hệ -->
        <div class="form-section">
          <div class="section-header">
            <i class="fas fa-address-book text-info"></i>
            <h6 class="mb-0">Thông tin liên hệ</h6>
          </div>
          <b-row>
            <b-col cols="12" md="6">
              <b-form-group label="Email" label-for="email">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-envelope"></i>
                  </b-input-group-prepend>
                  <b-form-input 
                    id="email" 
                    v-model.trim="companyForm.email" 
                    type="email"
                    placeholder="contact@example.com"
                  />
                </b-input-group>
              </b-form-group>
            </b-col>
            
            <b-col cols="12" md="6">
              <b-form-group label="Hotline" label-for="hotline">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-phone"></i>
                  </b-input-group-prepend>
                  <b-form-input 
                    id="hotline" 
                    v-model.trim="companyForm.hotline"
                    placeholder="1900 xxxx"
                  />
                </b-input-group>
              </b-form-group>
            </b-col>
          </b-row>
        </div>

        <!-- H�nh d?ng form -->
        <div class="form-actions">
          <b-button
            type="button"
            variant="secondary"
            size="sm"
            class="mr-2 btn-modal-cancel"
            @click="$refs.companyModal.hide()"
          >
            <i class="fas fa-times mr-1"></i>
            Hủy
          </b-button>
          <b-button
            type="submit"
            variant="primary"
            size="sm"
            class="btn-modal-save"
          >
            <i class="fas fa-save mr-1"></i>
            {{ companyForm.id ? 'Cập nhật' : 'Thêm mới' }}
          </b-button>
        </div>
      </b-form>
    </b-modal>
  </div>
</template>

<script>
import axios from "@/plugins/axios";

export default {
  name: "CompanyList",

  data() {
    return {
      isBusy: false,

      filter: {
        keyword: null,
        status: null,
        companyId: null
      },

      companyForm: {
        id: null,
        // giữ nguyên các trường cũ nếu có hiển thị ở bảng
        name: "",
        address: "",
        companyName: "",
        companyAddress: "",
        email: "",
        hotline: "",
        taxcode: "",
        status: 1
      },

      statusOptions: [
        { value: 1, text: "Kích hoạt" },
        { value: 0, text: "Ngưng hoạt động" }
      ],

      list: {
        current_page: 1,
        per_page: 10,
        total: 0
      },

      fields: [
        { key: "id", label: "#", thStyle: { width: "50px" }, class: "text-center" },
        { key: "company", label: "Tên công ty / Địa chỉ" },
        { key: "email", label: "Email" },
        { key: "status", label: "Trạng thái", class: "text-center" },
        { key: "option", label: "Thao tác", thStyle: { width: "80px" }, class: "text-center" }
      ],

      canCreateCompany: true,
      canUpdateCompany: true,
      canDeleteCompany: true,
    };
  },

  computed: {
    statusOptionsWithAll() {
      return [
        { value: null, text: "Tất cả trạng thái" },
        ...this.statusOptions
      ];
    }
  },

  methods: {
    dataProvider(ctx) {
      this.isBusy = true;
      return axios.post("/administrator/company/list", this.filter, {
        params: { page: ctx.currentPage - 1, size: this.list.per_page }
      })
      .then(res => {
        this.isBusy = false;
        this.list.total = res.data.totalElements;
        this.list.current_page = res.data.number + 1;
        return res.data.content || [];
      })
      .catch(err => {
        this.isBusy = false;
        const message = err.response?.data?.message || 'Không thể tải danh sách công ty';
        this.$toastr && this.$toastr.error(message);
        return [];
      });
    },

    onFilter() {
      this.list.current_page = 1;
      this.$refs.tblCompany.refresh();
    },

    onPageChange(page) {
      this.list.current_page = page;
      this.$refs.tblCompany.refresh();
    },

    showModal() {
      this.companyForm = {
        id: null,
        name: "",
        address: "",
        companyName: "",
        companyAddress: "",
        email: "",
        hotline: "",
        taxcode: "",
        status: 1
      };
      this.$refs.companyModal.show();
    },

    editCompany(item) {
      this.companyForm = { ...item };
      // giữ tương thích nếu API trả về companyName/companyAddress
      if (!this.companyForm.name && item.companyName) this.companyForm.name = item.companyName;
      if (!this.companyForm.address && item.companyAddress) this.companyForm.address = item.companyAddress;
      // chuẩn hóa trạng thái về 0/1: nếu thiếu mặc định 1 (kích hoạt)
      if (this.companyForm.status === undefined || this.companyForm.status === null || this.companyForm.status === '') {
        this.companyForm.status = 1;
      } else if (typeof this.companyForm.status === 'string') {
        // tương thích chuỗi từ backend
        this.companyForm.status = this.companyForm.status === 'active' ? 1 : 0;
      } else {
        this.companyForm.status = Number(this.companyForm.status) === 1 ? 1 : 0;
      }
      this.$refs.companyModal.show();
    },

    isActive(item) {
      const s = item && item.status;
      if (s === 'active') return true;
      if (s === 'inactive') return false;
      return Number(s) === 1;
    },

    saveCompany() {
      const payload = { ...this.companyForm };
      // chuẩn hóa tên/địa chỉ về trường backend
      if (payload.companyName && !payload.name) payload.name = payload.companyName;
      if (payload.companyAddress && !payload.address) payload.address = payload.companyAddress;
      // Trim các trường văn bản chính
      payload.name = (payload.name || '').trim();
      payload.address = (payload.address || '').trim();
      payload.email = (payload.email || '').trim();
      payload.taxcode = (payload.taxcode || '').trim();
      // Chuẩn hóa hotline: trim và set undefined nếu rỗng
      if (payload.hotline != null) {
        const hl = String(payload.hotline).trim();
        payload.hotline = hl.length > 0 ? hl : undefined;
      }
      // đảm bảo trạng thái mặc định là kích hoạt (1) nếu thiếu, và convert về số 0/1
      if (payload.status === undefined || payload.status === null || payload.status === '') {
        payload.status = 1;
      } else if (typeof payload.status === 'string') {
        payload.status = payload.status === 'active' ? 1 : (payload.status === 'inactive' ? 0 : Number(payload.status) || 0);
      } else {
        payload.status = Number(payload.status) === 1 ? 1 : 0;
      }
      axios.post("/administrator/company/saveOrUpdate", payload)
        .then(() => {
          this.$toastr && this.$toastr.success(payload.id ? 'Cập nhật công ty thành công' : 'Thêm công ty thành công');
          this.$refs.companyModal.hide();
          this.$refs.tblCompany.refresh();
        })
        .catch(err => {
          const message = err.response?.data?.message || 'Không thể lưu thông tin công ty';
          this.$toastr && this.$toastr.error(message);
        });
    },

    setCompanyStatus(id, status) {
      axios.post(`/administrator/company/${id}/status`, { status })
        .then(() => {
          this.$toastr && this.$toastr.success(status === 1 ? 'Đã kích hoạt công ty' : 'Đã tạm ngưng công ty');
          this.$refs.tblCompany.refresh();
        })
        .catch(err => {
          const message = err.response?.data?.message || 'Không thể cập nhật trạng thái công ty';
          this.$toastr && this.$toastr.error(message);
        });
    },

    deleteCompany(id) {
      if(confirm("Bạn có chắc muốn xóa công ty này?")) {
        axios.delete(`/administrator/company/${id}`)
          .then(() => {
            this.$toastr && this.$toastr.success('Đã xóa công ty');
            this.$refs.tblCompany.refresh();
          })
          .catch(err => {
            const message = err.response?.data?.message || 'Không thể xóa công ty';
            this.$toastr && this.$toastr.error(message);
          });
      }
    },

    sendAdminInfo(item) {
      axios.post(`/administrator/company/${item.id}/send-credentials`)
        .then(res => {
          const message = res?.data?.message || 'Đã gửi thông tin tài khoản quản trị tới email';
          this.$toastr && this.$toastr.success(message);
        })
        .catch(err => {
          const message = err.response?.data?.message || 'Không thể gửi thông tin tài khoản quản trị';
          this.$toastr && this.$toastr.error(message);
        });
    },

    safeRowNumber(index) {
      const page = Number(this.list && this.list.current_page) || 1;
      const size = Number(this.list && this.list.per_page) || 10;
      const idx = Number(index);
      return (isNaN(idx) ? 0 : idx) + 1 + (page - 1) * size;
    },
  }
};
</script>

<style scoped>
.permissions .card.shadow-sm { border-radius: 10px; }
.permissions .table-hover tbody tr:hover { background-color: #fafbfd; }
.permissions .btn-outline-primary { border-color: #dfe7ff; }
.permissions .btn-outline-primary:hover { background: #eef3ff; }

.permissions .table thead th { background: #f7f9fc; border-bottom: 1px solid #ecf0f6; color: #4a5568; font-weight: 700; }

/* Giữ tinh chỉnh bảng hiện đại hiện có */
.table-modern thead th { background-color: #f9fafb; border-bottom: 2px solid #e5e7eb; position: sticky; top: 0; z-index: 1; }
.table-compact td, .table-compact th { padding: 0.5rem 0.75rem; }
.table td { vertical-align: middle; }
.table-modern tbody tr:hover { background-color: #f6f8fa; }
.table-modern.table-striped tbody tr:nth-of-type(odd) { background-color: #fcfdff; }
.table-modern { border-color: #e9ecef; }
.text-mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }
.badge { font-size: 13px; }
.card { border: 1px solid #e9ecef; }
.card-body { padding: 0.75rem 1rem; }

/* Style modal c�ng ty */
.company-modal .modal-dialog {
  max-width: 800px;
}

.company-modal-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom: none;
  padding: 1.25rem 1.5rem;
}

.company-modal-header .modal-title {
  font-weight: 600;
  font-size: 1.25rem;
}

.company-modal-header .close {
  color: white;
  opacity: 0.9;
  text-shadow: none;
}

.company-modal-header .close:hover {
  opacity: 1;
}

.company-modal-body {
  padding: 1.5rem;
  background: #f8f9fa;
}

/* Khu v?c form */
.company-form .form-section {
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
  margin-bottom: 1.25rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  border: 1px solid #e9ecef;
}

.company-form .form-section:last-of-type {
  margin-bottom: 0;
}

.company-form .section-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 2px solid #f0f0f0;
}

.company-form .section-header i {
  font-size: 1.25rem;
}

.company-form .section-header h6 {
  font-weight: 600;
  color: #2d3748;
}

/* Nh�m form */
.company-form .form-group {
  margin-bottom: 1rem;
}

.company-form .form-group:last-child {
  margin-bottom: 0;
}

.company-form label {
  font-weight: 500;
  color: #4a5568;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.company-form .required-field label::after {
  content: ' *';
  color: #e53e3e;
}

/* Nh�m input */
.company-form .input-group-text {
  background: #f7fafc;
  border-right: none;
  color: #718096;
  min-width: 40px;
  justify-content: center;
}

.company-form .input-group .form-control {
  border-left: none;
}

.company-form .input-group .form-control:focus {
  border-color: #cbd5e0;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.1);
}

.company-form .input-group:focus-within .input-group-text {
  border-color: #cbd5e0;
  background: #edf2f7;
}

.company-form input.form-control,
.company-form select.form-control {
  border: 1px solid #e2e8f0;
  border-radius: 0.375rem;
  padding: 0.625rem 0.875rem;
  font-size: 0.9rem;
  transition: all 0.2s ease;
}

.company-form input.form-control:focus,
.company-form select.form-control:focus {
  outline: none;
  border-color: #4299e1;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.1);
}

.company-form .form-text {
  font-size: 0.8rem;
  color: #718096;
  margin-top: 0.375rem;
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.company-form .form-text i {
  font-size: 0.75rem;
}

/* H�nh d?ng form */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding-top: 1rem;
  margin-top: 1.25rem;
  border-top: 1px solid #e2e8f0;
}

.form-actions .btn {
  font-weight: 500;
  border-radius: 0.35rem;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.btn-modal-save {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.btn-modal-save:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.12);
}

.btn-modal-cancel {
  background-color: #f8fafc;
  border-color: #e2e8f0;
  color: #4a5568;
}

.btn-modal-cancel:hover {
  background-color: #e5e7eb;
  border-color: #cbd5e0;
  color: #111827;
}

@media (max-width: 768px) {
  .company-modal-body {
    padding: 1rem;
  }
  
  .company-form .form-section {
    padding: 1rem;
    margin-bottom: 1rem;
  }
  
  .company-form .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  
  .form-actions {
    flex-direction: row-reverse;
  }
  
  .form-actions .btn {
    width: 100%;
    justify-content: center;
  }
}

/* Hi?u ?ng */
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.company-form .form-section {
  animation: slideDown 0.3s ease;
}

.company-form .form-section:nth-child(1) {
  animation-delay: 0.05s;
}

.company-form .form-section:nth-child(2) {
  animation-delay: 0.1s;
}

.company-form .form-section:nth-child(3) {
  animation-delay: 0.15s;
}
</style>
