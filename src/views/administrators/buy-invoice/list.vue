<template>
  <div class="container-fluid py-3 buy-invoices">
    <!-- Tiêu đề và thao tác -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Danh sách hóa đơn mua</h4>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="onFilter">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="success" @click="showModal">
          <i class="fas fa-plus"></i>
          Thêm hóa đơn
        </b-button>
      </div>
    </div>

    <!-- Bộ lọc -->
    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="6" class="mb-2">
          <b-form-group label="Công ty:" horizontal :label-cols="3" label-class="font-weight-bold text-right">
            <v-select v-model="filter.companyId" :options="companyOptions" label="label" :reduce="c => c.value" placeholder="Chọn công ty" />
          </b-form-group>
        </b-col>
        <b-col md="6" class="mb-2">
          <b-form-group label="Trạng thái:" horizontal :label-cols="3" label-class="font-weight-bold text-right">
            <b-form-select v-model="filter.status" :options="statusOptions">
              <template #first>
                <b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option>
              </template>
            </b-form-select>
          </b-form-group>
        </b-col>
        <b-col md="12" class="text-right">
          <b-button size="sm" variant="primary" @click="onFilter">
            <i class="fas fa-search"></i> Tìm kiếm
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <!-- Bảng -->
    <b-card class="shadow-sm">
      <b-table
        bordered
        hover
        responsive="lg"
        small
        show-empty
        :fields="fields"
        :items="items"
        :busy="isBusy"
        empty-text="Không có dữ liệu"
        class="mb-0 table-modern table-compact"
      >
        <!-- STT -->
        <template #cell(id)="data">
          {{ data.index + 1 + (list.current_page - 1) * list.per_page }}
        </template>

        <template #cell(company)="data">
          <div class="font-weight-bold">{{ data.item.companyName || '—' }}</div>
        </template>

        <template #cell(amount)="data">
          <div class="d-flex align-items-center">
            <span class="mr-2 text-mono">{{ data.item.amountUsed }} / {{ data.item.amount }}</span>
            <b-progress height="8px" class="flex-grow-1" :value="data.item.amountUsed" :max="data.item.amount" :variant="data.item.amountUsed >= data.item.amount ? 'success' : 'info'" />
          </div>
        </template>

        <template #cell(status)="data">
          <b-badge :variant="statusVariant(data.item.status)">
            {{ statusText(data.item.status) }}
          </b-badge>
        </template>

        <template #cell(option)="data">
          <b-dropdown
            class="table-action-dropdown"
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
            <b-dropdown-item class="text-center" href="#" @click.prevent="editInvoice(data.item)">Cập nhật</b-dropdown-item>
            <b-dropdown-item 
              v-if="canDelete(data.item)"
              class="text-center text-danger" 
              href="#" 
              @click.prevent="deleteInvoice(data.item)"
            >
              Xóa
            </b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <pagination-bar
        :current.sync="list.current_page"
        :size.sync="list.per_page"
        :total="list.total"
        :sizes="pageSizes"
        @page-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </b-card>

    <!-- Hộp thoại -->
    <b-card class="mt-3 shadow-sm">
      <div class="d-flex align-items-center justify-content-between flex-wrap mb-3">
        <div>
          <h5 class="mb-1 font-weight-bold">Lịch sử thay đổi hạn mức</h5>
        </div>
        <b-button size="sm" variant="outline-primary" class="mt-2 mt-md-0" @click="loadHistories">
          <i class="fas fa-sync-alt mr-1"></i>
          Tải lại
        </b-button>
      </div>

      <b-row class="mb-2">
        <b-col md="3" class="mb-2">
          <v-select v-model="historyFilter.companyId" :options="companyOptions" label="label" :reduce="c => c.value" placeholder="Tất cả công ty" />
        </b-col>
        <b-col md="2" class="mb-2">
          <b-form-select v-model="historyFilter.source" :options="historySourceOptions" />
        </b-col>
        <b-col md="2" class="mb-2">
          <b-form-select v-model="historyFilter.changeType" :options="historyChangeTypeOptions" />
        </b-col>
        <b-col md="2" class="mb-2">
          <b-form-input v-model="historyFilter.fromDate" type="date" />
        </b-col>
        <b-col md="2" class="mb-2">
          <b-form-input v-model="historyFilter.toDate" type="date" />
        </b-col>
        <b-col md="1" class="mb-2 text-md-right">
          <b-button size="sm" variant="primary" block @click="applyHistoryFilter">
            Lọc
          </b-button>
        </b-col>
      </b-row>

      <b-table
        bordered
        hover
        responsive
        small
        show-empty
        :fields="historyFields"
        :items="histories"
        :busy="historyBusy"
        empty-text="Chưa có lịch sử thay đổi"
        class="mb-0 table-modern table-compact"
      >
        <template #cell(createdAt)="data">
          {{ formatDateTime(data.item.createdAt) }}
        </template>

        <template #cell(company)="data">
          <div class="font-weight-bold">{{ data.item.companyName || '—' }}</div>
          <small class="text-muted">{{ data.item.companyTaxcode || '—' }}</small>
        </template>

        <template #cell(changeType)="data">
          <b-badge :variant="historyChangeVariant(data.item.changeType)">
            {{ historyChangeText(data.item.changeType) }}
          </b-badge>
        </template>

        <template #cell(amountDelta)="data">
          <span class="font-weight-bold" :class="deltaClass(data.item.amountDelta)">
            {{ formatDelta(data.item.amountDelta) }}
          </span>
        </template>

        <template #cell(amount)="data">
          <span class="text-mono">{{ formatNumber(data.item.previousAmount) }} → {{ formatNumber(data.item.newAmount) }}</span>
        </template>

        <template #cell(source)="data">
          <b-badge :variant="historySourceVariant(data.item.source)">
            {{ historySourceText(data.item.source) }}
          </b-badge>
        </template>

        <template #cell(actor)="data">
          <div>{{ data.item.actorName || data.item.actorUsername || '—' }}</div>
          <small v-if="data.item.actorUsername" class="text-muted">{{ data.item.actorUsername }}</small>
        </template>

        <template #cell(note)="data">
          <div>{{ data.item.note || '—' }}</div>
          <small v-if="data.item.packageName || data.item.paymentCode" class="text-muted">
            {{ data.item.packageName || '—' }}<span v-if="data.item.paymentCode"> · {{ data.item.paymentCode }}</span>
          </small>
        </template>
      </b-table>

      <pagination-bar
        :current.sync="historyList.current_page"
        :size.sync="historyList.per_page"
        :total="historyList.total"
        :sizes="pageSizes"
        @page-change="onHistoryPageChange"
        @size-change="onHistoryPageSizeChange"
      />
    </b-card>

    <!-- Hộp thoại -->
    <b-modal ref="buyInvoiceModal" :title="invoiceForm.id ? 'Cập nhật hóa đơn' : 'Thêm hóa đơn'" hide-footer>
      <b-form novalidate @submit.prevent="saveInvoice">
        <b-form-group label="Công ty" label-class="font-weight-bold" :state="state('companyId')">
          <v-select 
            v-model="invoiceForm.companyId" 
            :options="companyOptions" 
            label="label" 
            :reduce="c => c.value" 
            :disabled="invoiceForm.id != null"
            placeholder="Chọn công ty"
            required
            :class="{ 'is-invalid': state('companyId') === false }"
          />
          <b-form-invalid-feedback :state="state('companyId')">
            {{ invalidFeedback('companyId') }}
          </b-form-invalid-feedback>
        </b-form-group>

        <b-form-group label="Số lượng" label-class="font-weight-bold" :state="state('amount')">
          <b-form-input type="number" v-model.number="invoiceForm.amount" required min="1" :state="state('amount')" />
          <b-form-invalid-feedback :state="state('amount')">
            {{ invalidFeedback('amount') }}
          </b-form-invalid-feedback>
          <small v-if="invoiceForm.id && invoiceForm.amountUsed > 0" class="form-text text-muted">
            <i class="fas fa-info-circle"></i> Đã sử dụng: <strong>{{ invoiceForm.amountUsed }}</strong> hóa đơn. 
            Số lượng phải ≥ {{ invoiceForm.amountUsed }}
          </small>
        </b-form-group>

        <b-form-group label="Trạng thái" label-class="font-weight-bold">
          <b-form-select v-model="invoiceForm.status" :options="statusOptions" />
        </b-form-group>

        <b-form-group label="Ghi chú thay đổi" label-class="font-weight-bold">
          <b-form-textarea v-model.trim="invoiceForm.note" rows="2" max-rows="4" placeholder="VD: Bổ sung theo yêu cầu khách hàng, điều chỉnh sai hạn mức..." />
        </b-form-group>

        <div class="text-right">
          <b-button type="submit" variant="primary" class="mr-2">Lưu</b-button>
          <b-button variant="secondary" @click="$refs.buyInvoiceModal.hide()">Hủy</b-button>
        </div>
      </b-form>
    </b-modal>
  </div>
</template>

<script>
import axios from "@/plugins/axios";
import { pageItems, pageTotal } from "@/utils/pagination";
import PaginationBar from "@/views/components/pagination_bar.vue";
import vSelect from "vue-select";
import "vue-select/dist/vue-select.css";

export default {
  name: "BuyInvoiceList",
  components: { vSelect, PaginationBar },

  data() {
    return {
      isBusy: false,
      historyBusy: false,
      items: [],
      histories: [],

      filter: {
        companyId: null,
        status: null
      },

      historyFilter: {
        companyId: null,
        source: null,
        changeType: null,
        fromDate: null,
        toDate: null
      },

      invoiceForm: {
        id: null,
        companyId: null,
        amount: 0,
        status: 1,
        note: ''
      },
      invoiceErrors: {},

      companyOptions: [],
      companyNameById: {},
      statusOptions: [
        { value: 1, text: "Kích hoạt" },
        { value: 0, text: "Ngưng hoạt động" }
      ],
      historySourceOptions: [
        { value: null, text: "Tất cả nguồn" },
        { value: "CUSTOMER", text: "Khách mua gói" },
        { value: "ADMIN", text: "Admin thao tác" }
      ],
      historyChangeTypeOptions: [
        { value: null, text: "Tất cả thao tác" },
        { value: "PACKAGE_PURCHASE", text: "Mua gói" },
        { value: "ADMIN_CREATE", text: "Admin thêm" },
        { value: "ADMIN_UPDATE", text: "Admin cập nhật" },
        { value: "ADMIN_DELETE", text: "Admin xóa" }
      ],

      list: {
        current_page: 1,
        per_page: 10,
        total: 0
      },
      historyList: {
        current_page: 1,
        per_page: 10,
        total: 0
      },
      pageSizes: [10, 20, 50, 100],

      fields: [
        { key: "id", label: "#", thStyle: { width: "50px" } },
        { key: "company", label: "Công ty" },
        { key: "amount", label: "Số lượng" },
        { key: "status", label: "Trạng thái" },
        { key: "option", label: "Chức năng", class: "text-center table-action-cell", thStyle: { width: "120px", minWidth: "120px" } }
      ],
      historyFields: [
        { key: "createdAt", label: "Thời gian", thStyle: { width: "145px" } },
        { key: "company", label: "Công ty", thStyle: { minWidth: "220px" } },
        { key: "changeType", label: "Thao tác", thStyle: { width: "130px" }, tdClass: "text-center" },
        { key: "amountDelta", label: "Tăng/Giảm", thStyle: { width: "110px" }, tdClass: "text-right" },
        { key: "amount", label: "Hạn mức", thStyle: { width: "150px" }, tdClass: "text-right" },
        { key: "source", label: "Nguồn", thStyle: { width: "125px" }, tdClass: "text-center" },
        { key: "actor", label: "Người thao tác", thStyle: { width: "150px" } },
        { key: "note", label: "Ghi chú", thStyle: { minWidth: "220px" } }
      ]
    };
  },

  computed: {
    canSubmit() {
      // Validate cơ bản
      if (!this.invoiceForm.companyId || !this.invoiceForm.amount || this.invoiceForm.amount <= 0) {
        return false;
      }
      
      // Khi chỉnh sửa, amount phải >= amountUsed
      if (this.invoiceForm.id && this.invoiceForm.amountUsed > 0) {
        return this.invoiceForm.amount >= this.invoiceForm.amountUsed;
      }
      
      return true;
    }
  },

  methods: {
    async loadData() {
      this.isBusy = true;
      try {
        const page = this.list.current_page - 1;
        const res = await axios.post(
          "/administrator/buy-invoice/list",
          this.filter,
          { params: { page, size: this.list.per_page } }
        );
        const data = res.data;
        this.list.total = pageTotal(data);
        const rawItems = pageItems(data);
        // Chuẩn hóa item và tin vào companyName backend trả về
        this.items = rawItems.map(item => ({
          ...item,
          companyId: Number.isFinite(Number(item.companyId)) ? Number(item.companyId) : undefined,
          companyName: item.companyName || '',
          amountUsed: item.amountUsed || 0
        }));
      } catch (e) {
        console.error(e);
        this.items = [];
      } finally {
        this.isBusy = false;
      }
    },
    async onFilter() {
      if (!this.companyOptions || this.companyOptions.length === 0) {
        await this.loadCompanies();
      }
      this.list.current_page = 1;
      await this.loadData();
    },
    onPageChange(page) {
      this.list.current_page = Number(page) || 1;
      this.loadData();
    },
    onPageSizeChange(size) {
      this.list.per_page = Number(size) || this.list.per_page;
      this.list.current_page = 1;
      this.loadData();
    },
    async loadHistories() {
      this.historyBusy = true;
      try {
        const page = this.historyList.current_page - 1;
        const res = await axios.post(
          "/administrator/buy-invoice/histories",
          this.normalizedHistoryFilter(),
          { params: { page, size: this.historyList.per_page } }
        );
        const data = res.data || {};
        this.histories = pageItems(data);
        this.historyList.total = pageTotal(data);
      } catch (e) {
        console.error(e);
        this.histories = [];
        this.historyList.total = 0;
        this.$toastr && this.$toastr.error('Không thể tải lịch sử thay đổi hạn mức');
      } finally {
        this.historyBusy = false;
      }
    },
    async applyHistoryFilter() {
      this.historyList.current_page = 1;
      await this.loadHistories();
    },
    onHistoryPageChange(page) {
      this.historyList.current_page = Number(page) || 1;
      this.loadHistories();
    },
    onHistoryPageSizeChange(size) {
      this.historyList.per_page = Number(size) || this.historyList.per_page;
      this.historyList.current_page = 1;
      this.loadHistories();
    },
    normalizedHistoryFilter() {
      return {
        companyId: this.historyFilter.companyId || null,
        source: this.historyFilter.source || null,
        changeType: this.historyFilter.changeType || null,
        fromDate: this.historyFilter.fromDate || null,
        toDate: this.historyFilter.toDate || null
      };
    },

    async showModal() {
      if (!this.companyOptions || this.companyOptions.length === 0) {
        await this.loadCompanies();
      }
      this.invoiceForm = { id: null, companyId: null, amount: 0, status: 1, note: '' };
      this.invoiceErrors = {};
      this.$refs.buyInvoiceModal.show();
    },

    async editInvoice(item) {
      if (!this.companyOptions || this.companyOptions.length === 0) {
        await this.loadCompanies();
      }
      // Copy các trường cần cho form, gồm amountUsed để validate
      this.invoiceForm = {
        id: item.id,
        companyId: item.companyId,
        amount: item.amount,
        amountUsed: item.amountUsed || 0,
        status: item.status,
        note: ''
      };
      this.invoiceErrors = {};
      this.$refs.buyInvoiceModal.show();
    },

    async saveInvoice() {
      if (!this.validateInvoiceForm()) return;
      try {
        await axios.post("/administrator/buy-invoice/create", this.invoiceForm);
        this.$toastr && this.$toastr.success(this.invoiceForm.id ? 'Cập nhật hóa đơn thành công' : 'Thêm hóa đơn thành công');
        this.$refs.buyInvoiceModal.hide();
        await this.loadData();
        await this.loadHistories();
      } catch (error) {
        const message = error.response?.data?.message || 'Không thể lưu hóa đơn';
        this.$toastr && this.$toastr.error(message);
      }
    },

    validateInvoiceForm() {
      const errors = {};
      if (!this.invoiceForm.companyId) {
        errors.companyId = ["Vui lòng chọn công ty"];
      }
      const amount = Number(this.invoiceForm.amount);
      if (!Number.isFinite(amount) || amount < 1) {
        errors.amount = ["Số lượng phải lớn hơn 0"];
      } else if (this.invoiceForm.id && Number(this.invoiceForm.amountUsed || 0) > 0 && amount < Number(this.invoiceForm.amountUsed)) {
        errors.amount = [`Số lượng phải lớn hơn hoặc bằng ${this.invoiceForm.amountUsed}`];
      }
      this.invoiceErrors = errors;
      return Object.keys(errors).length === 0;
    },

    state(field) {
      return Object.prototype.hasOwnProperty.call(this.invoiceErrors, field) ? false : null;
    },

    invalidFeedback(field) {
      const value = this.invoiceErrors[field];
      return Array.isArray(value) ? value.join(" ") : (value || "");
    },

    async deleteInvoice(item) {
      // Kiểm tra có thể xóa không
      if (!this.canDelete(item)) {
        this.$toastr && this.$toastr.warning(this.getDeleteTooltip(item));
        return;
      }
      
      if (!confirm("Xóa hóa đơn này?")) return;
      try {
        await axios.post("/administrator/buy-invoice/delete", { id: item.id });
        this.$toastr && this.$toastr.success('Đã xóa hóa đơn');
        await this.loadData();
        await this.loadHistories();
      } catch (error) {
        const message = error.response?.data?.message || 'Không thể xóa hóa đơn';
        this.$toastr && this.$toastr.error(message);
      }
    },

    canDelete(item) {
      // Không thể xóa nếu status = 1 (active) hoặc amountUsed > 0
      return item.status !== 1 && item.amountUsed === 0;
    },

    getDeleteTooltip(item) {
      if (item.status === 1) {
        return 'Không thể xóa gói đang kích hoạt. Vui lòng ngưng kích hoạt trước';
      }
      if (item.amountUsed > 0) {
        return 'Không thể xóa gói đã sử dụng (' + item.amountUsed + ' hóa đơn đã cấp số)';
      }
      return '';
    },

    async loadCompanies() {
      try {
        const res = await axios.post("/administrator/company/list", {}, {
          params: { page: 0, size: 5000 },
          meta: { suppressGlobalErrorToast: true },
        });
        const list = res.data?.content || [];
        const map = {};
        this.companyOptions = list.map(c => {
          const id = Number(c.id);
          const name = c.name || c.companyName || `#${id}`;
          map[id] = name;
          return { value: id, label: name };
        });
        this.companyNameById = map;
      } catch (e) {
        this.companyOptions = [];
        this.companyNameById = {};
      }
    },

    // Ánh xạ mã trạng thái sang chữ hiển thị
    statusText(s) {
      const n = Number(s)
      if (Number.isNaN(n)) return '—'
      const map = {
        1: 'Kích hoạt',
        0: 'Ngưng hoạt động'
      }
      return map[n] ?? '—'
    },
    // Ánh xạ mã trạng thái sang biến thể nhãn
    statusVariant(s) {
      const n = Number(s)
      if (Number.isNaN(n)) return 'light'
      return n === 1 ? 'success' : n === 0 ? 'secondary' : 'light'
    },
    historyChangeText(type) {
      const map = {
        PACKAGE_PURCHASE: 'Mua gói',
        ADMIN_CREATE: 'Admin thêm',
        ADMIN_UPDATE: 'Admin cập nhật',
        ADMIN_DELETE: 'Admin xóa'
      };
      return map[type] || type || '—';
    },
    historyChangeVariant(type) {
      const map = {
        PACKAGE_PURCHASE: 'success',
        ADMIN_CREATE: 'primary',
        ADMIN_UPDATE: 'warning',
        ADMIN_DELETE: 'danger'
      };
      return map[type] || 'secondary';
    },
    historySourceText(source) {
      const map = {
        CUSTOMER: 'Khách hàng',
        ADMIN: 'Admin'
      };
      return map[source] || source || '—';
    },
    historySourceVariant(source) {
      return source === 'CUSTOMER' ? 'success' : source === 'ADMIN' ? 'primary' : 'secondary';
    },
    formatNumber(value) {
      return new Intl.NumberFormat('vi-VN').format(Number(value || 0));
    },
    formatDelta(value) {
      const n = Number(value || 0);
      if (n > 0) return `+${this.formatNumber(n)}`;
      if (n < 0) return `-${this.formatNumber(Math.abs(n))}`;
      return '0';
    },
    deltaClass(value) {
      const n = Number(value || 0);
      if (n > 0) return 'text-success';
      if (n < 0) return 'text-danger';
      return 'text-muted';
    },
    formatDateTime(value) {
      if (!value) return '—';
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ');
      const dd = String(date.getDate()).padStart(2, '0');
      const mm = String(date.getMonth() + 1).padStart(2, '0');
      const yyyy = date.getFullYear();
      const hh = String(date.getHours()).padStart(2, '0');
      const mi = String(date.getMinutes()).padStart(2, '0');
      return `${dd}/${mm}/${yyyy} ${hh}:${mi}`;
    }
  },

  mounted: async function() {
    // Tải công ty trước để map tên sẵn sàng, sau đó tải hóa đơn
    await this.loadCompanies();
    await this.loadData();
    await this.loadHistories();
  }
};
</script>

<style scoped>
.buy-invoices .card.shadow-sm { border-radius: 10px; }
.buy-invoices .table-hover tbody tr:hover { background-color: #fafbfd; }
.buy-invoices .btn-outline-primary { border-color: #dfe7ff; }
.buy-invoices .btn-outline-primary:hover { background: #eef3ff; }

.buy-invoices .table thead th { background: #f7f9fc; border-bottom: 1px solid #ecf0f6; color: #4a5568; font-weight: 700; }

/* Tinh chỉnh bảng hiện đại để khớp danh sách công ty */
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

.v-select { width: 100%; }

::v-deep .v-select.is-invalid .vs__dropdown-toggle {
  border-color: #dc3545;
}

::v-deep .v-select.is-invalid .vs__dropdown-toggle:focus-within {
  box-shadow: 0 0 0 .2rem rgba(220, 53, 69, .25);
}
</style>
