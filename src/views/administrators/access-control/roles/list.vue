<template>
  <div class="container-fluid py-3 roles">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Danh sách vai trò</h4>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="onFilter">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="success" @click="showModal">
          <i class="fas fa-plus"></i>
          Thêm vai trò
        </b-button>
      </div>
    </div>

    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col cols="8">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input v-model="keyword" placeholder="Tìm theo tên / hiển thị" @keyup.enter="onFilter" />
          </b-input-group>
        </b-col>
        <b-col cols="4" class="text-right">
          <b-button size="sm" variant="primary" @click="onFilter">Tìm kiếm</b-button>
        </b-col>
      </b-row>
    </b-card>

    <b-card class="shadow-sm">
      <b-table bordered hover responsive small show-empty :items="items" :fields="fields" :busy="isBusy" empty-text="Không có dữ liệu">
        <template #cell(index)="data">{{ data.index + 1 + (list.current_page - 1) * list.per_page }}</template>
        <template #cell(permissions)="data">
          <div class="perm-chips">
            <b-badge v-for="p in data.item.permissions || []" :key="p.id" variant="info" class="perm-chip">{{ p.displayName || p.name }}</b-badge>
          </div>
        </template>
        <template #cell(option)="data">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content><i class="fas fa-ellipsis-h"></i></template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="editRole(data.item)">{{ $t('core.btn.update') || 'Cập nhật' }}</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="openClone(data.item)"><span>Clone</span></b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="deleteRole(data.item)"><span class="text-danger">{{ $t('core.btn.delete') || 'Xóa' }}</span></b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>
      <b-pagination v-if="list.total > list.per_page" v-model="list.current_page" :per-page="list.per_page" :total-rows="list.total" align="right" class="mt-2" @change="onPageChange" />
    </b-card>

    <!-- Modal Create/Update Role -->
    <b-modal
      ref="roleModal"
      :title="form.id ? 'Sửa vai trò' : 'Thêm vai trò'"
      hide-footer
      size="xl"
      content-class="role-modal-content"
      header-class="role-modal-header"
      body-class="role-modal-body"
    >
      <b-form @submit.prevent="saveRole">
        <b-card class="section-card mb-3">
          <b-row>
            <b-col cols="4">
              <b-form-group label="Tên vai trò"><b-form-input v-model="form.name" required /></b-form-group>
            </b-col>
            <b-col cols="4">
              <b-form-group label="Tên hiển thị"><b-form-input v-model="form.displayName" required /></b-form-group>
            </b-col>
            <b-col cols="4">
              <b-form-group label="Mô tả"><b-form-input v-model="form.description" /></b-form-group>
            </b-col>
          </b-row>
        </b-card>

        <b-card class="section-card">
          <div class="d-flex align-items-center mb-2">
            <h6 class="mb-0 mr-2"><i class="fas fa-key mr-1"></i> Phân quyền</h6>
            <b-badge variant="light" class="text-muted">Tích chọn để gán quyền</b-badge>
            <b-button size="sm" class="ml-auto" variant="outline-secondary" @click="selectAll(true)">Chọn tất cả</b-button>
            <b-button size="sm" class="ml-2" variant="outline-secondary" @click="selectAll(false)">Bỏ chọn</b-button>
          </div>

          <b-row>
            <b-col v-for="group in groupedPermissions" :key="group.id" cols="12" md="6" lg="4" class="mb-3">
              <b-card class="perm-group h-100">
                <div class="d-flex align-items-center mb-2 perm-group-header">
                  <strong>{{ group.name }}</strong>
                  <b-button size="sm" class="ml-auto" variant="link" @click="toggleGroup(group)">{{ group._allSelected ? 'Bỏ chọn nhóm' : 'Chọn nhóm' }}</b-button>
                </div>
                <div>
                  <!-- bind checkboxes to computed v-model array -->
                  <b-form-checkbox
                    v-for="p in group.items"
                    :key="p.id"
                    :value="p.id"
                    v-model="selectedPermissionIds"
                    switch
                    class="mb-1"
                  >
                    {{ p.displayName || p.name }}
                  </b-form-checkbox>
                </div>
              </b-card>
            </b-col>
          </b-row>
        </b-card>

        <div class="text-right mt-3">
          <b-button type="submit" variant="primary">Lưu</b-button>
          <b-button variant="secondary" @click="$refs.roleModal.hide()">Hủy</b-button>
        </div>
      </b-form>
    </b-modal>

    <!-- Modal Clone Role -->
    <b-modal
      ref="cloneModal"
      title="Clone vai trò"
      hide-footer
      content-class="role-modal-content"
      header-class="role-modal-header"
      body-class="role-modal-body"
    >
      <b-form @submit.prevent="doClone">
        <b-card class="section-card">
          <b-form-group label="Tên vai trò mới">
            <b-form-input v-model="cloneForm.name" required />
          </b-form-group>
          <b-form-group label="Tên hiển thị mới">
            <b-form-input v-model="cloneForm.displayName" required />
          </b-form-group>
        </b-card>
        <div class="text-right">
          <b-button type="submit" variant="primary">Clone</b-button>
          <b-button variant="secondary" @click="$refs.cloneModal.hide()">Hủy</b-button>
        </div>
      </b-form>
    </b-modal>
  </div>
</template>

<script>
import axios from "@/plugins/axios";

export default {
  name: "RolesList",
  data() {
    return {
      keyword: "",
      items: [],
      permissions: [],
      categories: [],
      isBusy: false,
      list: { current_page: 1, per_page: 10, total: 0 },
      form: { id: null, name: "", displayName: "", description: "", permissions: [] },
      cloneForm: { sourceId: null, name: "", displayName: "" },
      fields: [
        { key: "index", label: "#", thStyle: { width: "50px" } },
        { key: "name", label: "Tên" },
        { key: "displayName", label: "Hiển thị" },
        { key: "description", label: "Mô tả" },
        { key: "permissions", label: "Quyền" },
        { key: "option", label: "Chức năng", thStyle: { width: "80px" } }
      ]
    };
  },
  computed: {
    categoryById() {
      const map = {};
      for (const c of this.categories) map[c.id] = c;
      return map;
    },
    // v-model array for checkboxes: map form.permissions <-> ids
    selectedPermissionIds: {
      get() {
        return (this.form.permissions || []).map(x => x.id);
      },
      set(ids) {
        this.form.permissions = (ids || []).map(id => ({ id }));
      }
    },
    groupedPermissions() {
      const map = {};
      for (const p of this.permissions) {
        // Resolve category name from object or from ID via categories map
        const catId = p && typeof p.category === 'object' ? p.category?.id : p.category;
        const catName = (p && typeof p.category === 'object' && p.category?.name)
          ? p.category.name
          : (this.categoryById[catId]?.name || 'Khác');
        if (!map[catName]) map[catName] = { id: catName, name: catName, items: [] };
        if (p.status === 1) map[catName].items.push(p);
      }
      const groups = Object.values(map);
      const selected = new Set(this.selectedPermissionIds);
      for (const g of groups) {
        g._allSelected = g.items.length > 0 && g.items.every(x => selected.has(x.id));
      }
      return groups;
    }
  },
  mounted() {
    this.loadData();
    this.loadPermissions();
    this.loadCategories();
  },
  methods: {
    async loadData() {
      this.isBusy = true;
      try {
        const page = this.list.current_page - 1;
        const res = await axios.post("/administrator/roles/list", null, { params: { keyword: this.keyword, page, size: this.list.per_page } });
        this.items = res.data.content || [];
        this.list.total = res.data.totalElements || 0;
      } finally { this.isBusy = false; }
    },
    async loadPermissions() {
      const res = await axios.post("/administrator/permissions/list", null, { params: { page: 0, size: 1000 } });
      this.permissions = res.data.content || [];
    },
    async loadCategories() {
      const res = await axios.post("/administrator/permission-categories/list", null, { params: { page: 0, size: 1000 } });
      this.categories = res.data.content || [];
    },
    onFilter() { this.list.current_page = 1; this.loadData(); },
    onPageChange(p) { this.list.current_page = p; this.loadData(); },

    showModal() {
      this.form = { id: null, name: "", displayName: "", description: "", permissions: [] };
      this.$refs.roleModal.show();
    },
    editRole(r) {
      this.form = { id: r.id, name: r.name, displayName: r.displayName, description: r.description, permissions: (r.permissions||[]).map(p => ({ id: p.id })) };
      this.$refs.roleModal.show();
    },

    // Toggle a whole group on/off by syncing selectedPermissionIds
    toggleGroup(group) {
      const current = new Set(this.selectedPermissionIds);
      const ids = group.items.map(i => i.id);
      const allSelected = ids.every(id => current.has(id));
      if (allSelected) {
        for (const id of ids) current.delete(id);
      } else {
        for (const id of ids) current.add(id);
      }
      this.selectedPermissionIds = Array.from(current);
    },
    selectAll(flag) {
      if (flag) {
        const current = new Set(this.selectedPermissionIds);
        for (const p of this.permissions.filter(p => p.status === 1)) current.add(p.id);
        this.selectedPermissionIds = Array.from(current);
      } else {
        this.selectedPermissionIds = [];
      }
    },

    async saveRole() { await axios.post("/administrator/roles/saveOrUpdate", this.form); this.$refs.roleModal.hide(); this.loadData(); },
    async deleteRole(r) { if (!confirm("Xóa vai trò này?")) return; await axios.delete(`/administrator/roles/${r.id}`); this.loadData(); },

    openClone(r) { this.cloneForm = { sourceId: r.id, name: `${r.name}_copy`, displayName: `${r.displayName || r.name} (copy)` }; this.$refs.cloneModal.show(); },
    async doClone() { await axios.post(`/administrator/roles/${this.cloneForm.sourceId}/clone`, { name: this.cloneForm.name, displayName: this.cloneForm.displayName }); this.$refs.cloneModal.hide(); this.loadData(); }
  }
};
</script>

<style scoped>
.roles .card.shadow-sm { border-radius: 10px; }
.roles .table-hover tbody tr:hover { background-color: #fafbfd; }
.roles .btn-outline-primary { border-color: #dfe7ff; }
.roles .btn-outline-primary:hover { background: #eef3ff; }
.roles .perm-group { border: 1px solid #e8e8e8; border-radius: 10px; }

/* New polished styles */
.roles .table { border-radius: 10px; overflow: hidden; }
.roles .table thead th { background: #f7f9fc; border-bottom: 1px solid #ecf0f6; color: #4a5568; font-weight: 700; }
.roles .dropdown-toggle { color: #6b7280 !important; }
.roles .dropdown-toggle:hover { color: #374151 !important; }

.roles .perm-chips { display: flex; flex-wrap: wrap; gap: 6px; max-height: 56px; overflow: hidden; }
.roles .perm-chip.badge-info {
  background: #eef6ff; color: #1e40af; border: 1px solid #dbeafe; font-weight: 600; padding: 6px 10px; border-radius: 9999px;
}

/* Checkbox switches spacing inside permission groups */
.roles .perm-group .custom-control { padding-left: 2.25rem; }
.roles .perm-group .custom-control-label::before,
.roles .perm-group .custom-control-label::after { top: .2rem; }

/* Modal polish */
.roles .role-modal-content { border-radius: 14px; overflow: hidden; border: 1px solid #eef0f6; box-shadow: 0 10px 30px rgba(18, 38, 63, 0.08); }
.roles .role-modal-header { background: linear-gradient(180deg, #ffffff 0%, #f7f9fc 100%); border-bottom: 1px solid #ecf0f6; }
.roles .role-modal-body { background: #ffffff; max-height: 72vh; overflow: auto; padding-bottom: 8px; }

.roles .section-card { border: 1px solid #e8e8e8; border-radius: 12px; padding: 12px 14px; background: #fff; box-shadow: 0 1px 2px rgba(0,0,0,0.02); }
.roles .perm-group { border: 1px solid #e8e8e8; border-radius: 12px; }
.roles .perm-group-header { border-bottom: 1px dashed #ecf0f6; padding-bottom: 6px; margin-bottom: 8px; }

/* Form label & inputs subtle tone */
.roles .form-group > label { color: #4a5568; font-weight: 600; }
.roles .form-control { border-color: #e3e8f1; }
.roles .form-control:focus { border-color: #a8c1ff; box-shadow: 0 0 0 0.2rem rgba(74,108,247,.1); }

/* Link-like group toggle button */
.roles .btn-link { color: #4a6cf7; font-weight: 600; }
.roles .btn-link:hover { text-decoration: underline; }

/* Subtle hover for group items */
.roles .perm-group .custom-control:hover .custom-control-label { color: #1f2937; }
</style>