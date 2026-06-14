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

    <!-- B?ng -->
    <b-card class="shadow-sm">
      <b-table
        bordered
        hover
        responsive
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
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
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

      <!-- Phân trang -->
      <b-pagination v-if="list.total > list.per_page" v-model="list.current_page" :per-page="list.per_page" :total-rows="list.total" align="right" class="mt-2" @change="onPageChange" />
    </b-card>

    <!-- Hộp thoại -->
    <b-modal ref="buyInvoiceModal" :title="invoiceForm.id ? 'Cập nhật hóa đơn' : 'Thêm hóa đơn'" hide-footer>
      <b-form @submit.prevent="saveInvoice">
        <b-form-group label="Công ty" label-class="font-weight-bold">
          <v-select 
            v-model="invoiceForm.companyId" 
            :options="companyOptions" 
            label="label" 
            :reduce="c => c.value" 
            :disabled="invoiceForm.id != null"
            placeholder="Chọn công ty"
            required
          />
        </b-form-group>

        <b-form-group label="Số lượng" label-class="font-weight-bold">
          <b-form-input type="number" v-model.number="invoiceForm.amount" required min="1" />
          <small v-if="invoiceForm.id && invoiceForm.amountUsed > 0" class="form-text text-muted">
            <i class="fas fa-info-circle"></i> Đã sử dụng: <strong>{{ invoiceForm.amountUsed }}</strong> hóa đơn. 
            Số lượng phải ≥ {{ invoiceForm.amountUsed }}
          </small>
        </b-form-group>

        <b-form-group label="Trạng thái" label-class="font-weight-bold">
          <b-form-select v-model="invoiceForm.status" :options="statusOptions" />
        </b-form-group>

        <div class="text-right">
          <b-button type="submit" variant="primary" :disabled="!canSubmit">Lưu</b-button>
          <b-button variant="secondary" @click="$refs.buyInvoiceModal.hide()">Hủy</b-button>
        </div>
      </b-form>
    </b-modal>
  </div>
</template>

<script>
import axios from "@/plugins/axios";
import vSelect from "vue-select";
import "vue-select/dist/vue-select.css";

export default {
  name: "BuyInvoiceList",
  components: { vSelect },

  data() {
    return {
      isBusy: false,
      items: [],

      filter: {
        companyId: null,
        status: null
      },

      invoiceForm: {
        id: null,
        companyId: null,
        amount: 0,
        status: 1
      },

      companyOptions: [],
      companyNameById: {},
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
        { key: "id", label: "#", thStyle: { width: "50px" } },
        { key: "company", label: "Công ty" },
        { key: "amount", label: "Số lượng" },
        { key: "status", label: "Trạng thái" },
        { key: "option", label: "Chức năng", thStyle: { width: "150px" } }
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
        this.list.total = data.totalElements;
        const rawItems = Array.isArray(data.content) ? data.content : [];
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
      this.list.current_page = page;
      this.loadData();
    },

    async showModal() {
      if (!this.companyOptions || this.companyOptions.length === 0) {
        await this.loadCompanies();
      }
      this.invoiceForm = { id: null, companyId: null, amount: 0, status: 1 };
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
        status: item.status
      };
      this.$refs.buyInvoiceModal.show();
    },

    async saveInvoice() {
      try {
        await axios.post("/administrator/buy-invoice/create", this.invoiceForm);
        this.$toastr && this.$toastr.success(this.invoiceForm.id ? 'Cập nhật hóa đơn thành công' : 'Thêm hóa đơn thành công');
        this.$refs.buyInvoiceModal.hide();
        this.loadData();
      } catch (error) {
        const message = error.response?.data?.message || 'Không thể lưu hóa đơn';
        this.$toastr && this.$toastr.error(message);
      }
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
        this.loadData();
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
        const res = await axios.post("/administrator/company/list", {}, { params: { page: 0, size: 5000 } });
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
    }
  },

  mounted: async function() {
    // Tải công ty trước để map tên sẵn sàng, sau đó tải hóa đơn
    await this.loadCompanies();
    await this.loadData();
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
</style>
