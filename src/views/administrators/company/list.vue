<template>
  <div class="container-fluid py-3 permissions">

    <!-- Title + actions -->
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

    <!-- Filters Row -->
    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="4" class="mb-2">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input
              v-model.trim="filter.keyword"
              placeholder="Tìm theo Domain / MST / Tên"
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

    <!-- Table -->
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

        <template #cell(domain)="data">
          <div class="text-mono">{{ data.item.domain }}</div>
          <div class="text-muted small" v-if="data.item.domainLookup">Tra cứu: {{ data.item.domainLookup }}</div>
        </template>

        <template #cell(company)="data">
          <div class="font-weight-bold">{{ data.item.name || data.item.companyName }}</div>
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
          >
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item v-if="canUpdateCompany" class="text-center" href="#" @click.prevent="editCompany(data.item)">Cập nhật</b-dropdown-item>
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
    <b-modal id="companyModal" ref="companyModal" title="Thêm/Cập nhật công ty" hide-footer>
      <b-form @submit.prevent="saveCompany">
        <b-row>
          <b-col cols="12" md="6">
            <!-- Tên công ty -->
            <b-form-group label="Tên công ty:" label-for="name">
              <b-form-input id="name" v-model="companyForm.name" required />
            </b-form-group>
          </b-col>
          <b-col cols="12" md="6">
            <!-- Mã số thuế -->
            <b-form-group label="Mã số thuế:" label-for="taxcode">
              <b-form-input id="taxcode" v-model="companyForm.taxcode" required />
            </b-form-group>
          </b-col>

          <b-col cols="12">
            <!-- Địa chỉ -->
            <b-form-group label="Địa chỉ:" label-for="address">
              <b-form-input id="address" v-model="companyForm.address" />
            </b-form-group>
          </b-col>

          <b-col cols="12" md="6">
            <!-- Domain -->
            <b-form-group label="Domain:" label-for="domain">
              <b-form-input id="domain" v-model="companyForm.domain" required />
            </b-form-group>
          </b-col>
          <b-col cols="12" md="6">
            <!-- Domain tra cứu -->
            <b-form-group label="Domain tra cứu:" label-for="domainLookup">
              <b-form-input id="domainLookup" v-model="companyForm.domainLookup" />
            </b-form-group>
          </b-col>

          <b-col cols="12" md="6">
            <!-- Email -->
            <b-form-group label="Email:" label-for="email">
              <b-form-input id="email" v-model="companyForm.email" type="email" />
            </b-form-group>
          </b-col>
          <b-col cols="12" md="6">
            <!-- Hotline -->
            <b-form-group label="Hotline:" label-for="hotline">
              <b-form-input id="hotline" v-model="companyForm.hotline" />
            </b-form-group>
          </b-col>

          <b-col cols="12" md="6">
            <!-- Trạng thái -->
            <b-form-group label="Trạng thái:" label-for="status">
              <b-form-select id="status" v-model="companyForm.status" :options="statusOptions" />
            </b-form-group>
          </b-col>

          <b-col cols="12" class="text-right">
            <!-- Actions -->
            <b-button type="submit" variant="primary">Lưu</b-button>
            <b-button type="button" variant="secondary" @click="$refs.companyModal.hide()">Hủy</b-button>
          </b-col>
        </b-row>
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
        // các trường theo yêu cầu
        domain: "",
        domainLookup: "",
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
        { key: "domain", label: "Domain" },
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
      .catch(() => {
        this.isBusy = false;
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
        domain: "",
        domainLookup: "",
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
      payload.domain = (payload.domain || '').trim();
      payload.domainLookup = (payload.domainLookup || '').trim();
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
        });
    },

    setCompanyStatus(id, status) {
      axios.post(`/administrator/company/${id}/status`, { status })
        .then(() => {
          this.$toastr && this.$toastr.success(status === 1 ? 'Đã kích hoạt công ty' : 'Đã tạm ngưng công ty');
          this.$refs.tblCompany.refresh();
        });
    },

    deleteCompany(id) {
      if(confirm("Bạn có chắc muốn xóa công ty này?")) {
        axios.delete(`/administrator/company/${id}`).then(() => {
          this.$toastr && this.$toastr.success('Đã xóa công ty');
          this.$refs.tblCompany.refresh();
        });
      }
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

/* Keep existing table modern tweaks */
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
</style>