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

    <!-- FILTER -->
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

    <!-- DRAGGABLE LIST -->
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
                <div class="item-name">{{ item.name }}</div>
                <div class="small text-muted">STT: {{ idx + 1 }}</div>
              </div>
            </div>
            <div class="d-flex align-items-center">
              <b-form-checkbox switch size="sm" v-model="item.status" :value="1" :unchecked-value="0" @change="toggleStatus(item)">
                <span class="ml-1" :class="item.status === 1 ? 'text-success' : 'text-muted'">
                  {{ item.status === 1 ? 'Hiển thị' : 'Ẩn' }}
                </span>
              </b-form-checkbox>
              <b-button size="sm" variant="outline-secondary" class="ml-3" @click="editCategory(item)">
                <i class="fas fa-edit"></i>
              </b-button>
              <b-button size="sm" variant="outline-danger" class="ml-2" @click="deleteCategory(item)">
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

    <!-- TABLE (detailed) -->
    <b-card class="shadow-sm">
      <b-table
        bordered
        hover
        responsive
        small
        show-empty
        :items="items"
        :fields="fields"
        :busy="isBusy"
        empty-text="Không có dữ liệu"
      >
        <template #cell(index)="data">
          {{ data.index + 1 + (list.current_page - 1) * list.per_page }}
        </template>
        <template #cell(status)="data">
          <b-badge :variant="data.item.status === 1 ? 'success' : 'secondary'">
            {{ data.item.status === 1 ? 'Hiển thị' : 'Ẩn' }}
          </b-badge>
        </template>
        <template #cell(option)="data">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret v-if="true">
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="showModalUpdate(data.item.id)">
              Cập nhật
            </b-dropdown-item>
            <b-dropdown-item v-if="data.item.status === 0" class="text-center" href="#" @click.prevent="btnDelete(data.item.id)">
              <span class="text-danger">Xóa</span>
            </b-dropdown-item>
          </b-dropdown>
          <b-button size="sm" disabled variant="light" v-else>
            <i class="fas fa-user-lock"></i>
          </b-button>
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

    <!-- MODAL -->
    <b-modal ref="categoryModal" :title="form.id ? 'Sửa nhóm quyền' : 'Thêm nhóm quyền'" hide-footer>
      <b-form @submit.prevent="saveCategory">
        <b-form-group label="Tên nhóm">
          <b-form-input v-model="form.name" required />
        </b-form-group>
        <b-form-group label="Thứ tự hiển thị">
          <b-form-input type="number" v-model.number="form.orderIndex" />
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
import draggable from "vuedraggable";

export default {
  name: "PermissionCategoriesList",
  components: { draggable },
  data() {
    return {
      keyword: "",
      items: [],
      orderedItems: [],
      isBusy: false,
      dirtyOrder: false,
      savingOrder: false,
      list: { current_page: 1, per_page: 10, total: 0 },
      form: { id: null, name: "", orderIndex: 0, status: 1 },
      statusOptions: [
        { value: 1, text: "Hiển thị" },
        { value: 0, text: "Ẩn" }
      ],
      fields: [
        { key: "index", label: "#", thStyle: { width: "50px" } },
        { key: "name", label: "Tên nhóm" },
        { key: "orderIndex", label: "Thứ tự" },
        { key: "status", label: "Trạng thái" },
        { key: "option", label: "Chức năng", thStyle: { width: "150px" } }
      ]
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    async loadData() {
      this.isBusy = true;
      try {
        const page = this.list.current_page - 1;
        const tableReq = axios.post(
          "/administrator/permission-categories/list",
          null,
          { params: { keyword: this.keyword, page, size: this.list.per_page } }
        );
        const orderReq = axios.post(
          "/administrator/permission-categories/list",
          null,
          { params: { keyword: this.keyword, page: 0, size: 1000 } }
        );
        const [tableRes, orderRes] = await Promise.all([tableReq, orderReq]);

        this.items = tableRes.data.content || [];
        this.list.total = tableRes.data.totalElements || 0;

        const all = orderRes.data.content || [];
        this.orderedItems = [...all].sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0));
        this.dirtyOrder = false;
      } catch (e) {
        console.error(e);
        this.items = [];
        this.orderedItems = [];
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
    onDragEnd() {
      // mark as dirty so user can save
      // also update visible orderIndex for visual consistency
      this.orderedItems.forEach((it, i) => (it.orderIndex = i));
      this.dirtyOrder = true;
    },
    async saveOrder() {
      if (!this.dirtyOrder || this.savingOrder) return;
      this.savingOrder = true;
      try {
        const orderedIds = this.orderedItems.map(it => it.id);
        await axios.post("/administrator/permission-categories/reorder", orderedIds);
        this.dirtyOrder = false;
        this.loadData();
      } finally {
        this.savingOrder = false;
      }
    },
    showModal() {
      this.form = { id: null, name: "", orderIndex: 0, status: 1 };
      this.$refs.categoryModal.show();
    },
    editCategory(c) {
      this.form = { id: c.id, name: c.name, orderIndex: c.orderIndex, status: c.status };
      this.$refs.categoryModal.show();
    },
    async saveCategory() {
      await axios.post("/administrator/permission-categories/saveOrUpdate", this.form);
      this.$refs.categoryModal.hide();
      this.loadData();
    },
    async deleteCategory(c) {
      if (!confirm("Xóa nhóm quyền này?")) return;
      await axios.delete(`/administrator/permission-categories/${c.id}`);
      this.loadData();
    },
    async toggleStatus(item) {
      // inline quick toggle
      const payload = { id: item.id, name: item.name, orderIndex: item.orderIndex, status: item.status };
      await axios.post("/administrator/permission-categories/saveOrUpdate", payload);
    },
    showModalUpdate(id) {
      const found = this.items.find(x => x.id === id) || this.orderedItems.find(x => x.id === id)
      if (found) return this.editCategory(found)
      axios.get(`/administrator/permission-categories/${id}`).then(res => {
        if (res && res.data) this.editCategory(res.data)
      })
    },
    btnDelete(id) {
      const found = this.items.find(x => x.id === id) || this.orderedItems.find(x => x.id === id)
      if (found) return this.deleteCategory(found)
      if (!confirm("Xóa nhóm quyền này?")) return
      axios.delete(`/administrator/permission-categories/${id}`).then(() => this.loadData())
    }
  }
};
</script>

<style scoped>
.permission-categories .draggable-list { margin: 0; padding: 0; }
.permission-categories .draggable-item {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 10px;
  transition: box-shadow .2s ease, transform .1s ease;
}
.permission-categories .draggable-item:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,.06);
}
.permission-categories .drag-handle { cursor: grab; color: #999; }
.permission-categories .drag-handle:hover { color: #666; }
.permission-categories .item-name { font-weight: 600; }
.flip-list-move { transition: transform .2s ease; }
</style>