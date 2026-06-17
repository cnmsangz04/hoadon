<template>
  <div class="container-fluid py-3 vat-rates">
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
        <b-col md="6" class="mb-2 mb-md-0">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input
              v-model="filter.keyword"
              placeholder="Tìm theo mã / tên thuế suất"
              @keyup.enter="onFilter"
            />
          </b-input-group>
        </b-col>
        <b-col md="4" class="mb-2 mb-md-0">
          <b-form-select v-model="filter.status" :options="statusOptions">
            <template #first>
              <b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option>
            </template>
          </b-form-select>
        </b-col>
        <b-col md="2" class="text-right">
          <b-button size="sm" variant="primary" @click="onFilter">
            Tìm kiếm
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <!-- Danh sách sắp xếp -->
    <b-card class="vat-order-card shadow-sm">
      <div class="vat-order-toolbar">
        <div>
          <h6>Thứ tự hiển thị</h6>
          <p>Chọn 2 thuế suất để hoán đổi vị trí, hoặc kéo tay nắm để tinh chỉnh.</p>
        </div>
        <div class="vat-order-actions">
          <b-badge variant="light">{{ orderedItems.length }} thuế suất</b-badge>
          <b-badge v-if="dirtyOrder" variant="warning">Chưa lưu</b-badge>
          <b-button size="sm" :disabled="!dirtyOrder || savingOrder" variant="primary" @click="saveOrder">
            <i v-if="!savingOrder" class="fas fa-save mr-1"></i>
            <b-spinner v-else small class="mr-1" />
            Lưu sắp xếp
          </b-button>
        </div>
      </div>

      <div v-if="orderedItems.length > 1" class="quick-swap-panel">
        <div class="quick-swap-title">
          <i class="fas fa-random"></i>
          <span>Hoán đổi nhanh</span>
        </div>
        <b-form-select
          v-model="quickSwap.fromId"
          :options="quickSwapOptions"
          size="sm"
        />
        <i class="fas fa-exchange-alt quick-swap-icon"></i>
        <b-form-select
          v-model="quickSwap.toId"
          :options="quickSwapOptions"
          size="sm"
        />
        <b-button
          size="sm"
          variant="outline-primary"
          :disabled="!canQuickSwap"
          @click="swapQuickOrder"
        >
          Đổi chỗ
        </b-button>
      </div>

      <div class="vat-order-table">
        <div v-if="orderedItems.length" class="vat-order-head">
          <span></span>
          <span>STT</span>
          <span>Thuế suất</span>
          <span>Giá trị</span>
          <span>Trạng thái</span>
          <span>Chức năng</span>
        </div>

        <draggable
          v-model="orderedItems"
          :options="{ handle: '.drag-handle', animation: 150, ghostClass: 'drag-ghost' }"
          @end="onDragEnd"
          tag="div"
          class="vat-order-body"
        >
          <transition-group type="transition" name="flip-list" tag="div">
            <div
              v-for="(item, idx) in orderedItems"
              :key="item.id"
              class="vat-order-row"
            >
              <button type="button" class="drag-handle" title="Kéo để thay đổi thứ tự" aria-label="Kéo để thay đổi thứ tự">
                <i class="fas fa-grip-vertical"></i>
              </button>
              <div class="order-index">{{ idx + 1 }}</div>
              <div class="order-name">
                <strong>{{ item.label || formatLabel(item.code) }}</strong>
                <small>Mã thuế suất: #{{ item.id }}</small>
              </div>
              <div class="order-value">
                {{ item.code }}
              </div>
              <div class="order-status">
                <b-form-checkbox switch size="sm" v-model="item.status" :value="1" :unchecked-value="0" @change="toggleStatus(item, $event)">
                  {{ item.status === 1 ? 'Hiển thị' : 'Ẩn' }}
                </b-form-checkbox>
              </div>
              <div class="order-actions">
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
                  <b-dropdown-item class="text-center" href="#" @click.prevent="editVatRate(item)">
                    Cập nhật
                  </b-dropdown-item>
                  <b-dropdown-item class="text-center text-danger" href="#" @click.prevent="deleteVatRate(item.id)">
                    Xóa
                  </b-dropdown-item>
                </b-dropdown>
              </div>
            </div>
          </transition-group>
        </draggable>
      </div>

      <b-alert show variant="light" v-if="!orderedItems.length" class="text-center mb-0">
        Không có dữ liệu
      </b-alert>
    </b-card>

    <!-- Hộp thoại -->
    <b-modal ref="vatRateModal" :title="form.id ? 'Cập nhật thuế suất' : 'Thêm thuế suất'" hide-footer>
      <b-form novalidate @submit.prevent="saveVatRate">
        <b-form-group label="Thuế suất" :state="state('label')">
          <b-form-input v-model.trim="form.label" placeholder="Nhập tên/nhãn thuế suất" :state="state('label')" />
          <b-form-invalid-feedback :state="state('label')">
            {{ invalidFeedback('label') }}
          </b-form-invalid-feedback>
        </b-form-group>

        <b-form-group label="Giá trị" :state="state('code')">
          <b-form-input type="number" v-model.number="form.code" placeholder="Nhập giá trị" required :state="state('code')" />
          <b-form-invalid-feedback :state="state('code')">
            {{ invalidFeedback('code') }}
          </b-form-invalid-feedback>
        </b-form-group>

        <b-form-group label="Thứ tự hiển thị" :state="state('prioritize')">
          <b-form-input type="number" min="0" v-model.number="form.prioritize" placeholder="Nhập thứ tự hiển thị" :state="state('prioritize')" />
          <b-form-invalid-feedback :state="state('prioritize')">
            {{ invalidFeedback('prioritize') }}
          </b-form-invalid-feedback>
        </b-form-group>

        <b-form-group label="Trạng thái">
          <b-form-select v-model.number="form.status" :options="statusOptions" />
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

export default {
  name: "VatRateList",
  components: { draggable },

  data() {
    return {
      isBusy: false,
      orderedItems: [],
      dirtyOrder: false,
      savingOrder: false,
      quickSwap: {
        fromId: null,
        toId: null
      },

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
      errors: {},

      statusOptions: [
        { value: 1, text: "Hiển thị" },
        { value: 0, text: "Ẩn" }
      ]
    };
  },

  computed: {
    quickSwapOptions() {
      const placeholder = { value: null, text: "Chọn thuế suất" };
      return [
        placeholder,
        ...this.orderedItems.map((item, index) => ({
          value: item.id,
          text: `${index + 1}. ${item.label || this.formatLabel(item.code)}`
        }))
      ];
    },
    canQuickSwap() {
      return Boolean(this.quickSwap.fromId && this.quickSwap.toId && this.quickSwap.fromId !== this.quickSwap.toId);
    }
  },

  methods: {
    formatLabel(code) {
      if (code === null || code === "") return "";
      if (Number(code) === -1) return "KCT";
      return `${code}%`;
    },

    async loadData() {
      this.isBusy = true;
      try {
        const res = await axios.post(
          "/administrator/vat-rate/list",
          this.filter,
          { params: { page: 1, size: 1000 } }
        );
        const data = res.data;
        const allItems = Array.isArray(data.items) ? data.items : [];
        this.orderedItems = allItems.map(i => ({
          id: i.id,
          code: i.code,
          label: i.label || this.formatLabel(i.code),
          status: i.status,
          prioritize: i.prioritize || 0
        })).sort((a, b) => (a.prioritize || 0) - (b.prioritize || 0));
        this.quickSwap = { fromId: null, toId: null };
        this.dirtyOrder = false;
      } catch (e) {
        console.error(e);
        this.orderedItems = [];
      } finally {
        this.isBusy = false;
      }
    },

    async onFilter() {
      await this.loadData();
    },

    showModal() {
      this.form = { id: null, label: "", code: null, status: 1, prioritize: 0 };
      this.errors = {};
      this.$refs.vatRateModal.show();
    },

    editVatRate(item) {
      this.errors = {};
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
      if (!this.validateForm()) return;
      const payload = {
        id: this.form.id,
        code: this.form.code,
        label: (this.form.label || "").trim(),
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

    validateForm() {
      const errors = {};
      const code = Number(this.form.code);
      if (this.form.code === null || this.form.code === "" || !Number.isFinite(code)) {
        errors.code = ["Vui lòng nhập giá trị thuế suất"];
      } else if (code < -1 || code > 100) {
        errors.code = ["Giá trị thuế suất phải từ -1 đến 100"];
      }
      const prioritize = Number(this.form.prioritize || 0);
      if (!Number.isFinite(prioritize) || prioritize < 0) {
        errors.prioritize = ["Thứ tự hiển thị không được âm"];
      }
      if (String(this.form.label || "").trim().length > 100) {
        errors.label = ["Tên thuế suất không được vượt quá 100 ký tự"];
      }
      this.errors = errors;
      return Object.keys(errors).length === 0;
    },

    state(field) {
      return Object.prototype.hasOwnProperty.call(this.errors, field) ? false : null;
    },

    invalidFeedback(field) {
      const value = this.errors[field];
      return Array.isArray(value) ? value.join(" ") : (value || "");
    },

    async deleteVatRate(id) {
      if (!confirm("Xóa thuế suất này?")) return;
      await axios.delete(`/administrator/vat-rate/${id}`);
      this.$toastr && this.$toastr.success("Đã xóa thuế suất");
      this.loadData();
    },

    onDragEnd() {
      this.markOrderChanged();
    },

    markOrderChanged() {
      this.orderedItems.forEach((it, i) => (it.prioritize = i + 1));
      this.dirtyOrder = true;
    },

    swapQuickOrder() {
      if (!this.canQuickSwap) return;
      const fromIndex = this.orderedItems.findIndex(item => item.id === this.quickSwap.fromId);
      const toIndex = this.orderedItems.findIndex(item => item.id === this.quickSwap.toId);
      if (fromIndex < 0 || toIndex < 0) return;
      const nextItems = [...this.orderedItems];
      [nextItems[fromIndex], nextItems[toIndex]] = [nextItems[toIndex], nextItems[fromIndex]];
      this.orderedItems = nextItems;
      this.quickSwap = { fromId: null, toId: null };
      this.markOrderChanged();
    },

    async saveOrder() {
      if (!this.dirtyOrder || this.savingOrder) return;
      this.savingOrder = true;
      try {
        const orderedIds = this.orderedItems.map(it => it.id);
        await axios.post("/administrator/vat-rate/reorder", orderedIds);
        this.dirtyOrder = false;
        this.$toastr && this.$toastr.success("Đã lưu sắp xếp thuế suất");
        this.loadData();
      } finally {
        this.savingOrder = false;
      }
    },

    async toggleStatus(item, value) {
      // Bật/tắt nhanh inline
      item.status = Number(value);
      const payload = {
        id: item.id,
        label: item.label,
        code: item.code,
        status: item.status,
        prioritize: item.prioritize
      };
      await axios.put(`/administrator/vat-rate/${item.id}`, payload);
      this.$toastr && this.$toastr.success(item.status === 1 ? "Đã bật hiển thị" : "Đã ẩn thuế suất");
      this.loadData();
    }
  },

  mounted: async function() {
    await this.loadData();
  }
};
</script>

<style scoped>
.vat-order-card {
  border: 1px solid #e6ebf2;
  border-radius: 10px;
}

.vat-order-toolbar {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  margin-bottom: 12px;
}

.vat-order-toolbar h6 {
  font-size: 15px;
  font-weight: 800;
  margin: 0;
}

.vat-order-toolbar p {
  color: #64748b;
  font-size: 12px;
  margin: 4px 0 0;
}

.vat-order-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.quick-swap-panel {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  display: grid;
  gap: 6px;
  grid-template-columns: auto minmax(180px, 1fr) auto minmax(180px, 1fr) auto;
  margin-bottom: 12px;
  padding: 8px 10px;
}

.quick-swap-panel ::v-deep .custom-select,
.quick-swap-panel ::v-deep .btn {
  height: 30px;
  min-height: 30px;
  padding-bottom: 4px;
  padding-top: 4px;
}

.quick-swap-title {
  align-items: center;
  color: #334155;
  display: inline-flex;
  font-weight: 800;
  gap: 8px;
  white-space: nowrap;
}

.quick-swap-title i,
.quick-swap-icon {
  color: #2563eb;
}

.quick-swap-icon {
  text-align: center;
}

.vat-order-table {
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  overflow: hidden;
}

.vat-order-head,
.vat-order-row {
  display: grid;
  gap: 8px;
  grid-template-columns: 38px 58px minmax(180px, 1fr) 110px 140px 96px;
}

.vat-order-head {
  background: #f8fafc;
  border-bottom: 1px solid #e6ebf2;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  padding: 8px 12px;
}

.vat-order-row {
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #eef2f7;
  min-height: 46px;
  padding: 7px 12px;
  transition: background-color .15s ease, box-shadow .15s ease;
}

.vat-order-row:last-child {
  border-bottom: 0;
}

.vat-order-row:hover {
  background: #fbfdff;
}

.vat-order-row.drag-ghost {
  background: #eff6ff;
  box-shadow: inset 3px 0 0 #2563eb;
}

.drag-handle {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #dbe3ef;
  border-radius: 7px;
  color: #64748b;
  cursor: grab;
  display: inline-flex;
  height: 28px;
  justify-content: center;
  width: 28px;
}

.drag-handle:hover {
  background: #eef4ff;
  border-color: #bfdbfe;
  color: #2563eb;
}

.drag-handle:active {
  cursor: grabbing;
}

.order-index {
  align-items: center;
  background: #f1f5f9;
  border-radius: 999px;
  color: #334155;
  display: inline-flex;
  font-weight: 800;
  height: 26px;
  justify-content: center;
  width: 38px;
}

.order-name {
  min-width: 0;
}

.order-name strong {
  color: #1f2937;
  display: block;
  font-weight: 700;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.order-name small {
  color: #94a3b8;
  display: block;
  margin-top: 2px;
}

.order-value {
  color: #334155;
  font-family: "Courier New", Courier, monospace;
  font-weight: 700;
}

.order-status {
  color: #334155;
  font-weight: 600;
}

.order-actions {
  align-items: center;
  display: flex;
  justify-content: center;
}

.order-actions ::v-deep .table-action-dropdown .dropdown-toggle {
  height: 28px;
  min-height: 28px;
  min-width: 28px;
  padding: 0;
  width: 28px;
}

.order-actions ::v-deep .table-action-dropdown .dropdown-menu {
  min-width: 140px;
}

.flip-list-move {
  transition: transform .2s ease;
}

@media (max-width: 768px) {
  .vat-order-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .vat-order-actions {
    justify-content: flex-start;
  }

  .quick-swap-panel {
    grid-template-columns: 1fr;
  }

  .quick-swap-icon {
    display: none;
  }

  .vat-order-head {
    display: none;
  }

  .vat-order-row {
    gap: 8px;
    grid-template-columns: 38px 52px minmax(0, 1fr);
  }

  .order-value,
  .order-status,
  .order-actions {
    grid-column: 3;
  }

  .order-actions {
    justify-content: flex-start;
  }
}
</style>
