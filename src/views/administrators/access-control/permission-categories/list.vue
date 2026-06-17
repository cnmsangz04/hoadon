<template>
  <div class="container-fluid py-3 permission-categories">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Nhóm quyền</h4>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="onFilter">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="success" @click="showModal">
          <i class="fas fa-plus"></i>
          Thêm nhóm
        </b-button>
      </div>
    </div>

    <!-- Bộ lọc -->
    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col cols="8">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input
              v-model="keyword"
              placeholder="Tìm theo tên nhóm"
              @keyup.enter="onFilter"
            />
          </b-input-group>
        </b-col>
        <b-col cols="4" class="text-right">
          <b-button size="sm" variant="primary" @click="onFilter">
            Tìm kiếm
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <!-- Danh sách kéo thả -->
    <b-card class="permission-order-card shadow-sm">
      <div class="permission-order-toolbar">
        <div>
          <h6>Thứ tự hiển thị</h6>
          <p>Chọn 2 nhóm quyền để hoán đổi vị trí, hoặc kéo tay nắm để tinh chỉnh.</p>
        </div>
        <div class="permission-order-actions">
          <b-badge variant="light">{{ orderedItems.length }} nhóm</b-badge>
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

      <div class="permission-order-table">
        <div v-if="orderedItems.length" class="permission-order-head">
          <span></span>
          <span>STT</span>
          <span>Tên nhóm</span>
          <span>Trạng thái</span>
          <span>Chức năng</span>
        </div>

        <draggable
          v-model="orderedItems"
          :options="{ handle: '.drag-handle', animation: 150, ghostClass: 'drag-ghost' }"
          @end="onDragEnd"
          tag="div"
          class="permission-order-body"
        >
          <transition-group type="transition" name="flip-list" tag="div">
            <div
              v-for="(item, idx) in orderedItems"
              :key="item.id"
              class="permission-order-row"
            >
              <button type="button" class="drag-handle" title="Kéo để thay đổi thứ tự" aria-label="Kéo để thay đổi thứ tự">
                <i class="fas fa-grip-vertical"></i>
              </button>
              <div class="order-index">{{ idx + 1 }}</div>
              <div class="order-name">
                <strong>{{ item.name }}</strong>
                <small>Mã nhóm: #{{ item.id }}</small>
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
                  <b-dropdown-item class="text-center" href="#" @click.prevent="editCategory(item)">
                    Cập nhật
                  </b-dropdown-item>
                  <b-dropdown-item class="text-center text-danger" href="#" @click.prevent="deleteCategory(item)">
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
    <b-modal ref="categoryModal" :title="form.id ? 'Cập nhật nhóm quyền' : 'Thêm nhóm quyền'" hide-footer>
      <b-form novalidate @submit.prevent="saveCategory">
        <b-form-group label="Tên nhóm" :state="state('name')">
          <b-form-input v-model.trim="form.name" required :state="state('name')" />
          <b-form-invalid-feedback :state="state('name')">
            {{ invalidFeedback('name') }}
          </b-form-invalid-feedback>
        </b-form-group>
        <b-form-group label="Thứ tự hiển thị" :state="state('orderIndex')">
          <b-form-input type="number" min="0" v-model.number="form.orderIndex" :state="state('orderIndex')" />
          <b-form-invalid-feedback :state="state('orderIndex')">
            {{ invalidFeedback('orderIndex') }}
          </b-form-invalid-feedback>
        </b-form-group>
        <b-form-group label="Trạng thái">
          <b-form-select v-model.number="form.status" :options="statusOptions" />
        </b-form-group>
        <div class="text-right">
          <b-button type="submit" variant="primary">Lưu</b-button>
          <b-button variant="secondary" @click="$refs.categoryModal.hide()">Hủy</b-button>
        </div>
      </b-form>
    </b-modal>
  </div>
</template>

<script>
import axios from "@/plugins/axios";
import { pageItems } from "@/utils/pagination";
import draggable from "vuedraggable";

export default {
  name: "PermissionCategoriesList",
  components: { draggable },
  data() {
    return {
      keyword: "",
      orderedItems: [],
      isBusy: false,
      dirtyOrder: false,
      savingOrder: false,
      quickSwap: {
        fromId: null,
        toId: null
      },
      form: { id: null, name: "", orderIndex: 0, status: 1 },
      errors: {},
      statusOptions: [
        { value: 1, text: "Hiển thị" },
        { value: 0, text: "Ẩn" }
      ]
    };
  },
  computed: {
    quickSwapOptions() {
      const placeholder = { value: null, text: "Chọn nhóm quyền" };
      return [
        placeholder,
        ...this.orderedItems.map((item, index) => ({
          value: item.id,
          text: `${index + 1}. ${item.name}`
        }))
      ];
    },
    canQuickSwap() {
      return Boolean(this.quickSwap.fromId && this.quickSwap.toId && this.quickSwap.fromId !== this.quickSwap.toId);
    }
  },
  mounted() {
    this.loadData();
  },
  methods: {
    async loadData() {
      this.isBusy = true;
      try {
        const res = await axios.post(
          "/administrator/permission-categories/list",
          null,
          { params: { keyword: this.keyword, page: 0, size: 1000 } }
        );
        const all = pageItems(res.data);
        this.orderedItems = [...all].sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0));
        this.quickSwap = { fromId: null, toId: null };
        this.dirtyOrder = false;
      } catch (e) {
        console.error(e);
        this.orderedItems = [];
      } finally {
        this.isBusy = false;
      }
    },
    onFilter() {
      this.loadData();
    },
    onDragEnd() {
      this.markOrderChanged();
    },
    markOrderChanged() {
      this.orderedItems.forEach((it, i) => (it.orderIndex = i + 1));
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
        await axios.post("/administrator/permission-categories/reorder", orderedIds);
        this.dirtyOrder = false;
        this.$toastr && this.$toastr.success('Đã lưu sắp xếp nhóm quyền');
        this.loadData();
      } finally {
        this.savingOrder = false;
      }
    },
    showModal() {
      this.form = { id: null, name: "", orderIndex: 0, status: 1 };
      this.errors = {};
      this.$refs.categoryModal.show();
    },
    editCategory(c) {
      this.form = { id: c.id, name: c.name, orderIndex: c.orderIndex, status: c.status };
      this.errors = {};
      this.$refs.categoryModal.show();
    },
    async saveCategory() {
      if (!this.validateForm()) return;
      await axios.post("/administrator/permission-categories/saveOrUpdate", this.form);
      this.$toastr && this.$toastr.success(this.form.id ? 'Cập nhật nhóm quyền thành công' : 'Thêm nhóm quyền thành công');
      this.$refs.categoryModal.hide();
      this.loadData();
    },
    validateForm() {
      const errors = {};
      if (!String(this.form.name || "").trim()) {
        errors.name = ["Vui lòng nhập tên nhóm"];
      }
      const orderIndex = Number(this.form.orderIndex || 0);
      if (!Number.isFinite(orderIndex) || orderIndex < 0) {
        errors.orderIndex = ["Thứ tự hiển thị không được âm"];
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
    async deleteCategory(c) {
      if (!confirm("Xóa nhóm quyền này?")) return;
      await axios.delete(`/administrator/permission-categories/${c.id}`);
      this.$toastr && this.$toastr.success('Đã xóa nhóm quyền');
      this.loadData();
    },
    async toggleStatus(item, value) {
      // Bật/tắt nhanh inline
      item.status = Number(value);
      const payload = { id: item.id, name: item.name, orderIndex: item.orderIndex, status: item.status };
      await axios.post("/administrator/permission-categories/saveOrUpdate", payload);
      this.$toastr && this.$toastr.success(item.status === 1 ? 'Đã bật hiển thị' : 'Đã ẩn nhóm');
    }
  }
};
</script>

<style scoped>
.permission-order-card {
  border: 1px solid #e6ebf2;
  border-radius: 10px;
}

.permission-order-toolbar {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  margin-bottom: 12px;
}

.permission-order-toolbar h6 {
  font-size: 15px;
  font-weight: 800;
  margin: 0;
}

.permission-order-toolbar p {
  color: #64748b;
  font-size: 12px;
  margin: 4px 0 0;
}

.permission-order-actions {
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

.permission-order-table {
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  overflow: hidden;
}

.permission-order-head,
.permission-order-row {
  display: grid;
  gap: 8px;
  grid-template-columns: 38px 58px minmax(220px, 1fr) 140px 96px;
}

.permission-order-head {
  background: #f8fafc;
  border-bottom: 1px solid #e6ebf2;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  padding: 8px 12px;
}

.permission-order-row {
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #eef2f7;
  min-height: 46px;
  padding: 7px 12px;
  transition: background-color .15s ease, box-shadow .15s ease;
}

.permission-order-row:last-child {
  border-bottom: 0;
}

.permission-order-row:hover {
  background: #fbfdff;
}

.permission-order-row.drag-ghost {
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
  .permission-order-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .permission-order-actions {
    justify-content: flex-start;
  }

  .quick-swap-panel {
    grid-template-columns: 1fr;
  }

  .quick-swap-icon {
    display: none;
  }

  .permission-order-head {
    display: none;
  }

  .permission-order-row {
    gap: 8px;
    grid-template-columns: 38px 52px minmax(0, 1fr);
  }

  .order-status,
  .order-actions {
    grid-column: 3;
  }

  .order-actions {
    justify-content: flex-start;
  }
}
</style>
