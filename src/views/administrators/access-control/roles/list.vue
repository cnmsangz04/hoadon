<template>
  <div class="container-fluid py-3">
    <h4 class="mb-3 font-weight-bold">Danh sách vai trò</h4>

    <!-- FILTER -->
    <b-card class="mb-2">
      <b-row>
        <b-col cols="6">
          <b-form-group
            label="Từ khóa:"
            horizontal
            :label-cols="3"
            label-class="font-weight-bold text-right"
          >
            <b-form-input
              v-model="keyword"
              placeholder="Tìm theo tên / hiển thị"
              @keyup.enter="onFilter"
            />
          </b-form-group>
        </b-col>

        <b-col cols="6" class="text-right">
          <b-button size="sm" variant="primary" @click="onFilter">
            <i class="fas fa-search"></i> Tìm kiếm
          </b-button>
          <b-button size="sm" variant="success" class="ml-2" @click="showModal">
            <i class="fas fa-plus"></i> Thêm vai trò
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
      :items="items"
      :fields="fields"
      :busy="isBusy"
      empty-text="Không có dữ liệu"
    >
      <!-- STT -->
      <template #cell(index)="data">
        {{ data.index + 1 + (list.current_page - 1) * list.per_page }}
      </template>

      <template #cell(permissions)="data">
        <b-badge
          v-for="p in data.item.permissions || []"
          :key="p.id"
          variant="info"
          class="mr-1 mb-1"
        >
          {{ p.displayName || p.name }}
        </b-badge>
      </template>

      <template #cell(option)="data">
        <b-button
          size="sm"
          variant="warning"
          class="mr-1"
          @click="editRole(data.item)"
        >
          Sửa
        </b-button>
        <b-button
          size="sm"
          variant="danger"
          @click="deleteRole(data.item)"
        >
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
    <b-modal
      ref="roleModal"
      :title="form.id ? 'Sửa vai trò' : 'Thêm vai trò'"
      hide-footer
      size="lg"
    >
      <b-form @submit.prevent="saveRole">
        <b-row>
          <b-col cols="6">
            <b-form-group label="Tên vai trò">
              <b-form-input v-model="form.name" required />
            </b-form-group>
          </b-col>

          <b-col cols="6">
            <b-form-group label="Tên hiển thị">
              <b-form-input v-model="form.displayName" required />
            </b-form-group>
          </b-col>
        </b-row>

        <b-form-group label="Mô tả">
          <b-form-input v-model="form.description" />
        </b-form-group>

        <b-form-group label="Permissions">
          <b-input-group>
            <b-form-select
              v-model="selectedPermissionId"
              :options="permissionOptions"
            />
            <b-input-group-append>
              <b-button variant="success" @click="addPermission">
                Thêm
              </b-button>
            </b-input-group-append>
          </b-input-group>
        </b-form-group>

        <div class="mb-3">
          <b-badge
            v-for="p in selectedPermissions"
            :key="p.id"
            variant="primary"
            class="mr-1 mb-1"
          >
            {{ p.displayName || p.name }}
            <span
              class="ml-1"
              style="cursor:pointer"
              @click="removePermission(p)"
            >
              ×
            </span>
          </b-badge>
        </div>

        <div class="text-right">
          <b-button type="submit" variant="primary">Lưu</b-button>
          <b-button variant="secondary" @click="$refs.roleModal.hide()">
            Hủy
          </b-button>
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
      selectedPermissionId: null,
      isBusy: false,

      list: {
        current_page: 1,
        per_page: 10,
        total: 0
      },

      form: {
        id: null,
        name: "",
        displayName: "",
        description: "",
        permissions: []
      },

      fields: [
        { key: "index", label: "#", thStyle: { width: "50px" } },
        { key: "name", label: "Tên" },
        { key: "displayName", label: "Hiển thị" },
        { key: "description", label: "Mô tả" },
        { key: "permissions", label: "Quyền" },
        { key: "option", label: "Chức năng", thStyle: { width: "150px" } }
      ]
    };
  },

  computed: {
    permissionOptions() {
      return this.permissions.map(p => ({
        value: p.id,
        text: p.displayName || p.name
      }));
    },

    selectedPermissions() {
      return this.permissions.filter(p =>
        this.form.permissions.some(x => x.id === p.id)
      );
    }
  },

  mounted() {
    this.loadData();
    this.loadPermissions();
  },

  methods: {
    async loadData() {
      this.isBusy = true;
      try {
        const page = this.list.current_page - 1;
        const res = await axios.post(
          "/administrator/roles/list",
          null,
          {
            params: {
              keyword: this.keyword,
              page,
              size: this.list.per_page
            }
          }
        );
        this.items = res.data.content || [];
        this.list.total = res.data.totalElements || 0;
      } finally {
        this.isBusy = false;
      }
    },

    async loadPermissions() {
      const res = await axios.post(
        "/administrator/permissions/list",
        null,
        { params: { page: 0, size: 1000 } }
      );
      this.permissions = res.data.content || [];
      if (!this.selectedPermissionId && this.permissions.length) {
        this.selectedPermissionId = this.permissions[0].id;
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
      this.form = {
        id: null,
        name: "",
        displayName: "",
        description: "",
        permissions: []
      };
      this.$refs.roleModal.show();
    },

    editRole(r) {
      this.form = {
        id: r.id,
        name: r.name,
        displayName: r.displayName,
        description: r.description,
        permissions: (r.permissions || []).map(p => ({ id: p.id }))
      };
      this.$refs.roleModal.show();
    },

    addPermission() {
      if (!this.selectedPermissionId) return;
      const exists = this.form.permissions.some(
        p => p.id === this.selectedPermissionId
      );
      if (!exists) {
        this.form.permissions.push({ id: this.selectedPermissionId });
      }
    },

    removePermission(p) {
      this.form.permissions = this.form.permissions.filter(
        x => x.id !== p.id
      );
    },

    async saveRole() {
      await axios.post("/administrator/roles/saveOrUpdate", this.form);
      this.$refs.roleModal.hide();
      this.loadData();
    },

    async deleteRole(r) {
      if (!confirm("Xóa vai trò này?")) return;
      await axios.delete(`/administrator/roles/${r.id}`);
      this.loadData();
    }
  }
};
</script>

<style scoped>
</style>
