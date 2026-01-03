<template>
  <div class="container-fluid py-3 buy-invoices">
    <!-- Title + actions -->
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

    <!-- FILTER -->
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

    <!-- TABLE -->
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
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="editInvoice(data.item)">Cập nhật</b-dropdown-item>
            <b-dropdown-item class="text-center text-danger" href="#" @click.prevent="deleteInvoice(data.item.id)">Xóa</b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <!-- PAGINATION -->
      <b-pagination v-if="list.total > list.per_page" v-model="list.current_page" :per-page="list.per_page" :total-rows="list.total" align="right" class="mt-2" @change="onPageChange" />
    </b-card>

    <!-- MODAL -->
    <b-modal ref="buyInvoiceModal" title="Thêm / Cập nhật hóa đơn" hide-footer>
      <b-form @submit.prevent="saveInvoice">
        <b-form-group label="Công ty">
          <v-select v-model="invoiceForm.companyId" :options="companyOptions" label="label" :reduce="c => c.value" :disabled="invoiceForm.id != null" />
        </b-form-group>

        <b-form-group label="Số lượng">
          <b-form-input type="number" v-model.number="invoiceForm.amount" required />
        </b-form-group>

        <b-form-group label="Trạng thái">
          <b-form-select v-model="invoiceForm.status" :options="statusOptions" />
        </b-form-group>

        <div class="text-right">
          <b-button type="submit" variant="primary">Lưu</b-button>
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
        // Normalize items trusting backend-provided companyName
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

    showModal() {
      this.invoiceForm = { id: null, companyId: null, amount: 0, status: 1 };
      this.$refs.buyInvoiceModal.show();
    },

    editInvoice(item) {
      this.invoiceForm = { ...item };
      this.$refs.buyInvoiceModal.show();
    },

    async saveInvoice() {
      await axios.post("/administrator/buy-invoice/create", this.invoiceForm);
      this.$toastr && this.$toastr.success(this.invoiceForm.id ? 'Cập nhật hóa đơn thành công' : 'Thêm hóa đơn thành công');
      this.$refs.buyInvoiceModal.hide();
      this.loadData();
    },

    async deleteInvoice(id) {
      if (!confirm("Xóa hóa đơn này?")) return;
      await axios.post("/administrator/buy-invoice/delete", { id });
      this.$toastr && this.$toastr.success('Đã xóa hóa đơn');
      this.loadData();
    },

    async loadCompanies() {
      try {
        const res = await axios.post("/administrator/company/list", {}, { params: { page: 0, size: 5000 } });
        const list = res.data?.content || [];
        const map = {};
        this.companyOptions = list.map(c => {
          const id = Number(c.id);
          const name = c.name || c.companyName || c.domain || `#${id}`;
          map[id] = name;
          return { value: id, label: name };
        });
        this.companyNameById = map;
      } catch (e) {
        this.companyOptions = [];
        this.companyNameById = {};
      }
    },

    // Map status code to label text
    statusText(s) {
      const n = Number(s)
      if (Number.isNaN(n)) return '—'
      const map = {
        1: 'Kích hoạt',
        0: 'Ngưng hoạt động'
      }
      return map[n] ?? '—'
    },
    // Map status code to badge variant
    statusVariant(s) {
      const n = Number(s)
      if (Number.isNaN(n)) return 'light'
      return n === 1 ? 'success' : n === 0 ? 'secondary' : 'light'
    }
  },

  mounted: async function() {
    // Load companies first so names map is ready, then invoices
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

/* Modern table tweaks to match company list */
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