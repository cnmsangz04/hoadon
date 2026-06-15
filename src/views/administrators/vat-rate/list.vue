<template>
  <div class="container-fluid py-3 vat-rates">
    <!-- Tiêu đề và thao tác -->
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

    <!-- Bộ lọc -->
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

    <!-- Danh sách kéo thả -->
    <b-card class="mb-2 shadow-sm">
      <div class="d-flex align-items-center mb-2">
        <h6 class="mb-0 mr-2">Thứ tự hiển thị</h6>
        <b-badge variant="light" class="text-muted">Kéo thả để sắp xếp</b-badge>
        <b-button size="sm" class="ml-auto" :disabled="!dirtyOrder || savingOrder" variant="warning" @click="saveOrder">
          <i v-if="!savingOrder" class="fas fa-save"></i>
          <b-spinner v-else small />
          <span class="ml-1">Lưu sắp xếp</span>
        </b-button>
      </div>

      <draggable
        v-model="orderedItems"
        :options="{ handle: '.drag-handle', animation: 150 }"
        @end="onDragEnd"
        tag="ul"
        class="list-unstyled mb-0 draggable-list"
      >
        <transition-group type="transition" name="flip-list">
          <li
            v-for="(item, idx) in orderedItems"
            :key="item.id"
            class="draggable-item d-flex align-items-center justify-content-between"
          >
            <div class="d-flex align-items-center">
              <span class="drag-handle mr-3" title="Kéo để thay đổi thứ tự">
                <i class="fas fa-grip-vertical"></i>
              </span>
              <div>
                <div class="item-name">{{ item.label || formatLabel(item.code) }}</div>
                <div class="small text-muted">STT: {{ idx + 1 }}</div>
              </div>
            </div>
            <div class="d-flex align-items-center">
              <b-form-checkbox switch size="sm" v-model="item.status" :value="1" :unchecked-value="0" @change="toggleStatus(item)">
                <span class="ml-1" :class="item.status === 1 ? 'text-success' : 'text-muted'">
                  {{ item.status === 1 ? 'Hiển thị' : 'Ẩn' }}
                </span>
              </b-form-checkbox>
              <b-button size="sm" variant="outline-secondary" class="ml-3" @click="editVatRate(item)">
                <i class="fas fa-edit"></i>
              </b-button>
              <b-button size="sm" variant="outline-danger" class="ml-2" @click="deleteVatRate(item.id)">
                <i class="fas fa-trash"></i>
              </b-button>
            </div>
          </li>
        </transition-group>
      </draggable>

      <b-alert show variant="light" v-if="!orderedItems.length" class="text-center mb-0">
        Không có dữ liệu
      </b-alert>
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
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="editVatRate(data.item)">Chỉnh sửa</b-dropdown-item>
            <b-dropdown-item class="text-center text-danger" href="#" @click.prevent="deleteVatRate(data.item.id)">Xóa</b-dropdown-item>
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
    <b-modal ref="vatRateModal" title="Thêm / Cập nhật thuế suất" hide-footer>
      <b-form @submit.prevent="saveVatRate">

        <b-form-group label="Thuế suất">
          <b-form-input v-model.trim="form.label" placeholder="Nhập tên/nhãn thuế suất" />
        </b-form-group>
        
        <b-form-group label="Giá trị">
          <b-form-input type="number" v-model.number="form.code" placeholder="Nhập giá trị" required />
        </b-form-group>

        <b-form-group label="Thứ tự hiển thị">
          <b-form-input type="number" v-model.number="form.prioritize" placeholder="Nhập thứ tự hiển thị" />
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
import draggable from "vuedraggable";
import PaginationBar from "@/views/components/pagination_bar.vue";

export default {
  name: "VatRateList",
  components: { draggable, PaginationBar },

  data() {
    return {
      isBusy: false,
      items: [],
      orderedItems: [],
      dirtyOrder: false,
      savingOrder: false,

      filter: {
        keyword: "",
        status: null
      },

      form: {
        id: null,
        label: "",
        code: null,
        status: 1,
        prioritize: 0
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
      pageSizes: [10, 20, 50, 100],

      fields: [
        { key: "id", label: "#", thStyle: { width: "50px" } },
        { key: "label", label: "Thuế suất" },
        { key: "code", label: "Giá trị", thStyle: { width: "120px" } },
        { key: "prioritize", label: "Thứ tự", thStyle: { width: "100px" } },
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
        const page = this.list.current_page; // gửi index trang bắt đầu từ 1
        const tableReq = axios.post(
          "/administrator/vat-rate/list",
          this.filter,
          { params: { page, size: this.list.per_page } }
        );
        const orderReq = axios.post(
          "/administrator/vat-rate/list",
          this.filter,
          { params: { page: 1, size: 1000 } }
        );
        
        const [tableRes, orderRes] = await Promise.all([tableReq, orderReq]);
        
        const data = tableRes.data;
        // Chuyển theo cấu trúc { items, total, per_page, current_page, last_page }
        const rawItems = Array.isArray(data.items) ? data.items : [];
        this.items = rawItems.map(i => ({
          id: i.id,
          code: i.code,
          label: i.label || this.formatLabel(i.code),
          status: i.status,
          prioritize: i.prioritize || 0
        }));
        this.list.total = Number(data.total) || 0;
        this.list.per_page = Number(data.per_page) || this.list.per_page;
        this.list.current_page = Number(data.current_page) || this.list.current_page;
        
        // Tải toàn bộ item để sắp xếp kéo thả
        const allData = orderRes.data;
        const allItems = Array.isArray(allData.items) ? allData.items : [];
        this.orderedItems = allItems.map(i => ({
          id: i.id,
          code: i.code,
          label: i.label || this.formatLabel(i.code),
          status: i.status,
          prioritize: i.prioritize || 0
        })).sort((a, b) => (a.prioritize || 0) - (b.prioritize || 0));
        this.dirtyOrder = false;
      } catch (e) {
        console.error(e);
        this.items = [];
        this.orderedItems = [];
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

    onPageSizeChange(size) {
      this.list.per_page = Number(size) || this.list.per_page;
      this.list.current_page = 1;
      this.loadData();
    },

    showModal() {
      this.form = { id: null, label: "", code: null, status: 1, prioritize: 0 };
      this.$refs.vatRateModal.show();
    },

    editVatRate(item) {
      this.form = { 
        id: item.id, 
        label: item.label, 
        code: item.code, 
        status: item.status,
        prioritize: item.prioritize || 0
      };
      this.$refs.vatRateModal.show();
    },

    async saveVatRate() {
      const payload = {
        id: this.form.id,
        code: this.form.code,
        label: (this.form.label || '').trim(),
        status: this.form.status,
        prioritize: this.form.prioritize || 0
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
    },

    onDragEnd() {
      // Đánh dấu đã thay đổi để user có thể lưu
      // Đồng thời cập nhật prioritize hiển thị để nhất quán giao diện
      this.orderedItems.forEach((it, i) => (it.prioritize = i));
      this.dirtyOrder = true;
    },

    async saveOrder() {
      if (!this.dirtyOrder || this.savingOrder) return;
      this.savingOrder = true;
      try {
        const orderedIds = this.orderedItems.map(it => it.id);
        await axios.post("/administrator/vat-rate/reorder", orderedIds);
        this.dirtyOrder = false;
        this.$toastr && this.$toastr.success('Đã lưu sắp xếp thuế suất');
        this.loadData();
      } finally {
        this.savingOrder = false;
      }
    },

    async toggleStatus(item) {
      // Bật/tắt nhanh inline
      const payload = { 
        id: item.id, 
        label: item.label, 
        code: item.code, 
        status: item.status,
        prioritize: item.prioritize 
      };
      await axios.put(`/administrator/vat-rate/${item.id}`, payload);
      this.$toastr && this.$toastr.success(item.status === 1 ? 'Đã bật hiển thị' : 'Đã ẩn thuế suất');
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

.draggable-list {
  margin: 0;
  padding: 0;
}

.draggable-item {
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 12px 15px;
  margin-bottom: 8px;
  cursor: default;
  transition: all 0.2s ease;
}

.draggable-item:hover {
  background: #f8f9fa;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.drag-handle {
  cursor: move;
  color: #999;
  font-size: 18px;
  user-select: none;
}

.drag-handle:hover {
  color: #666;
}

.item-name {
  font-weight: 600;
  color: #333;
}

.flip-list-move {
  transition: transform 0.3s;
}
</style>
