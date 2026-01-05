<template>
  <div class="container-fluid py-3 vat-rates">
    <!-- Title + actions -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Danh sách thuế suất</h4>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="onFilter">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="success" @click="showModal">
          <i class="fas fa-plus"></i>
          Thêm mới
        </b-button>
      </div>
    </div>

    <!-- FILTER -->
    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="6" class="mb-2">
          <b-form-group label="Từ khóa:" horizontal :label-cols="3" label-class="font-weight-bold text-right">
            <b-form-input v-model="filter.keyword" placeholder="Tìm theo mã / tên thuế suất" />
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

        <!-- Thuế suất label -->
        <template #cell(label)="data">
          <div class="font-weight-bold">{{ data.item.label || formatLabel(data.item.code) }}</div>
        </template>

        <!-- Giá trị (code) -->
        <template #cell(code)="data">
          <div class="text-mono">
            <span>{{ data.item.code }}</span>
          </div>
        </template>

        <!-- Trạng thái -->
        <template #cell(status)="data">
          <b-badge :variant="statusVariant(data.item.status)">
            {{ statusText(data.item.status) }}
          </b-badge>
        </template>

        <!-- Thao tác -->
        <template #cell(option)="data">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="editVatRate(data.item)">Chỉnh sửa</b-dropdown-item>
            <b-dropdown-item class="text-center text-danger" href="#" @click.prevent="deleteVatRate(data.item.id)">Xóa</b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>

      <!-- PAGINATION -->
      <b-pagination v-if="list.total > list.per_page" v-model="list.current_page" :per-page="list.per_page" :total-rows="list.total" align="right" class="mt-2" @input="onPageChange" />
    </b-card>

    <!-- MODAL -->
    <b-modal ref="vatRateModal" title="Thêm / Cập nhật thuế suất" hide-footer>
      <b-form @submit.prevent="saveVatRate">

        <b-form-group label="Thuế suất">
          <b-form-input v-model.trim="form.label" placeholder="Nhập tên/nhãn thuế suất" />
        </b-form-group>
        
        <b-form-group label="Giá trị">
          <b-form-input type="number" v-model.number="form.code" placeholder="Nhập giá trị" required />
        </b-form-group>

        <b-form-group label="Trạng thái">
          <b-form-select v-model="form.status" :options="statusOptions" />
        </b-form-group>

        <div class="text-right">
          <b-button type="submit" variant="primary">Lưu</b-button>
          <b-button variant="secondary" @click="$refs.vatRateModal.hide()">Hủy</b-button>
        </div>
      </b-form>
    </b-modal>
  </div>
</template>

<script>
import axios from "@/plugins/axios";

export default {
  name: "VatRateList",

  data() {
    return {
      isBusy: false,
      items: [],

      filter: {
        keyword: "",
        status: null
      },

      form: {
        id: null,
        label: "",
        code: null,
        status: 1
      },

      statusOptions: [
        { value: 1, text: "Hiển thị" },
        { value: 0, text: "Ẩn" }
      ],

      list: {
        current_page: 1,
        per_page: 10,
        total: 0
      },

      fields: [
        { key: "id", label: "#", thStyle: { width: "50px" } },
        { key: "label", label: "Thuế suất" },
        { key: "code", label: "Giá trị", thStyle: { width: "120px" } },
        { key: "status", label: "Trạng thái", thStyle: { width: "150px" } },
        { key: "option", label: "Chức năng", thStyle: { width: "150px" } }
      ]
    };
  },

  methods: {
    formatLabel(code) {
      if (code === null || code === "") return "";
      if (Number(code) === -1) return "KCT";
      return `${code}%`;
    },

    statusText(s) {
      const n = Number(s);
      if (Number.isNaN(n)) return "—";
      const map = {
        1: "Hiển thị",
        0: "Ẩn"
      };
      return map[n] ?? "—";
    },

    statusVariant(s) {
      const n = Number(s);
      if (Number.isNaN(n)) return "light";
      return n === 1 ? "success" : n === 0 ? "secondary" : "light";
    },

    async loadData() {
      this.isBusy = true;
      try {
        const page = this.list.current_page; // send 1-based page index
        const res = await axios.post(
          "/administrator/vat-rate/list",
          this.filter,
          { params: { page, size: this.list.per_page } }
        );
        const data = res.data;
        // Adapt to { items, total, per_page, current_page, last_page }
        const rawItems = Array.isArray(data.items) ? data.items : [];
        this.items = rawItems.map(i => ({
          id: i.id,
          code: i.code,
          label: i.label || this.formatLabel(i.code),
          status: i.status
        }));
        this.list.total = Number(data.total) || 0;
        this.list.per_page = Number(data.per_page) || this.list.per_page;
        this.list.current_page = Number(data.current_page) || this.list.current_page;
      } catch (e) {
        console.error(e);
        this.items = [];
        this.list.total = 0;
      } finally {
        this.isBusy = false;
      }
    },

    async onFilter() {
      this.list.current_page = 1;
      await this.loadData();
    },

    onPageChange(page) {
      this.list.current_page = page;
      this.loadData();
    },

    showModal() {
      this.form = { id: null, label: "", code: null, status: 1 };
      this.$refs.vatRateModal.show();
    },

    editVatRate(item) {
      this.form = { id: item.id, label: item.label, code: item.code, status: item.status };
      this.$refs.vatRateModal.show();
    },

    async saveVatRate() {
      const payload = {
        id: this.form.id,
        code: this.form.code,
        label: (this.form.label || '').trim(),
        status: this.form.status
      };
      if (!payload.label) {
        payload.label = this.formatLabel(this.form.code);
      }
      if (this.form.id == null) {
        await axios.post("/administrator/vat-rate", payload);
        this.$toastr && this.$toastr.success("Thêm thuế suất thành công");
      } else {
        await axios.put(`/administrator/vat-rate/${this.form.id}`, payload);
        this.$toastr && this.$toastr.success("Cập nhật thuế suất thành công");
      }
      this.$refs.vatRateModal.hide();
      this.loadData();
    },

    async deleteVatRate(id) {
      if (!confirm("Xóa thuế suất này?")) return;
      await axios.delete(`/administrator/vat-rate/${id}`);
      this.$toastr && this.$toastr.success("Đã xóa thuế suất");
      this.loadData();
    }
  },

  mounted: async function() {
    await this.loadData();
  }
};
</script>

<style scoped>
.table-modern .b-table-thead {
  background-color: #f8f9fa;
}

.table-modern .b-table-thead th {
  border-bottom: 2px solid #dee2e6;
}

.table-modern .b-table-tbody tr:hover {
  background-color: #f1f1f1;
}

.table-modern .b-table-tbody td {
  border-top: 1px solid #dee2e6;
}

.text-mono {
  font-family: "Courier New", Courier, monospace;
  font-size: 90%;
}
</style>