<template>
  <div class="container-fluid py-3">
    <h4 class="mb-3 font-weight-bold">Danh sách hóa đơn mua</h4>

    <!-- FILTER -->
    <b-card class="mb-2">
      <b-row>
        <b-col cols="6">
          <b-form-group
            label="Công ty:"
            horizontal
            :label-cols="3"
            label-class="font-weight-bold text-right"
          >
            <v-select
              v-model="filter.companyId"
              :options="companyOptions"
              label="label"
              :reduce="c => c.value"
              placeholder="Chọn công ty"
            />
          </b-form-group>
        </b-col>

        <b-col cols="6">
          <b-form-group
            label="Trạng thái:"
            horizontal
            :label-cols="3"
            label-class="font-weight-bold text-right"
          >
            <b-form-select
              v-model="filter.status"
              :options="statusOptions"
            />
          </b-form-group>
        </b-col>

        <b-col cols="12" class="text-right">
          <b-button size="sm" variant="primary" @click="onFilter">
            <i class="fas fa-search"></i> Tìm kiếm
          </b-button>
          <b-button size="sm" variant="success" class="ml-2" @click="showModal">
            <i class="fas fa-plus"></i> Thêm hóa đơn
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <!-- TABLE -->
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
    >
      <!-- STT -->
      <template #cell(id)="data">
        {{ data.index + 1 + (list.current_page - 1) * list.per_page }}
      </template>

      <template #cell(company)="data">
        <strong>{{ data.item.companyName }}</strong>
      </template>

      <template #cell(amount)="data">
        <span>{{ data.item.amountUsed }} / {{ data.item.amount }}</span>
        <b-progress
          height="8px"
          class="mt-1"
          :value="data.item.amountUsed"
          :max="data.item.amount"
          :variant="data.item.amountUsed >= data.item.amount ? 'success' : 'info'"
        />
      </template>

      <template #cell(status)="data">
        <b-badge :variant="data.item.status === 1 ? 'success' : 'secondary'">
          {{ data.item.status === 1 ? 'Kích hoạt' : 'Ngưng hoạt động' }}
        </b-badge>
      </template>

      <template #cell(option)="data">
        <b-button size="sm" variant="warning" class="mr-1" @click="editInvoice(data.item)">
          Sửa
        </b-button>
        <b-button size="sm" variant="danger" @click="deleteInvoice(data.item.id)">
          Xóa
        </b-button>
      </template>
    </b-table>

    <!-- PAGINATION -->
    <b-pagination
      v-if="list.total > list.per_page"
      v-model="list.current_page"
      :per-page="list.per_page"
      :total-rows="list.total"
      align="right"
      class="mt-2"
      @change="onPageChange"
    />

    <!-- MODAL -->
    <b-modal ref="buyInvoiceModal" title="Thêm / Sửa hóa đơn" hide-footer>
      <b-form @submit.prevent="saveInvoice">
        <b-form-group label="Công ty">
          <v-select
            v-model="invoiceForm.companyId"
            :options="companyOptions"
            label="label"
            :reduce="c => c.value"
            :disabled="invoiceForm.id != null"
          />
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

        this.items = data.content.map(item => ({
          ...item,
          companyName: item.company?.domain || "N/A",
          amountUsed: item.amountUsed || 0
        }));
      } catch (e) {
        console.error(e);
        this.items = [];
      } finally {
        this.isBusy = false;
      }
    },

    onFilter() {
      this.list.current_page = 1;
      this.loadData();
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
      const res = await axios.post("/administrator/company/list", {});
      this.companyOptions = res.data.content.map(c => ({
        value: c.id,
        label: c.domain
      }));
    }
  },

  mounted() {
    this.loadCompanies();
    this.loadData();
  }
};
</script>

<style scoped>
.v-select {
  width: 100%;
}
</style>