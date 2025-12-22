<template>
  <div class="container-fluid py-3">
    <h4 class="mb-3 font-weight-bold">Danh sách quyền</h4>

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
            <i class="fas fa-plus"></i> Thêm quyền
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

      <template #cell(category)="data">
        {{ data.item.category?.name || data.item.category }}
      </template>

      <template #cell(level)="data">
        <b-badge variant="info">Level {{ data.item.level }}</b-badge>
      </template>

      <template #cell(option)="data">
        <b-button
          size="sm"
          variant="warning"
          class="mr-1"
          @click="editPermission(data.item)"
        >
          Sửa
        </b-button>
        <b-button
          size="sm"
          variant="danger"
          @click="deletePermission(data.item)"
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
      ref="permissionModal"
      :title="form.id ? 'Sửa quyền' : 'Thêm quyền'"
      hide-footer
    >
      <b-form @submit.prevent="savePermission">
        <b-form-group label="Tên quyền">
          <b-form-input v-model="form.name" required />
        </b-form-group>

        <b-form-group label="Tên hiển thị">
          <b-form-input v-model="form.displayName" required />
        </b-form-group>

        <b-form-group label="Level">
          <b-form-input
            type="number"
            min="0"
            max="2"
            v-model.number="form.level"
          />
        </b-form-group>

        <b-form-group label="Nhóm quyền">
          <b-form-select
            v-model="form.category"
            :options="categoryOptions"
            required
          />
        </b-form-group>

        <b-form-group label="Mô tả">
          <b-form-textarea v-model="form.description" rows="2" />
        </b-form-group>

        <div class="text-right">
          <b-button type="submit" variant="primary">Lưu</b-button>
          <b-button variant="secondary" @click="$refs.permissionModal.hide()">
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
  name: "PermissionsList",

  data() {
    return {
      keyword: "",
      items: [],
      categories: [],
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
        level: 0,
        category: null,
        description: ""
      },

      fields: [
        { key: "index", label: "#", thStyle: { width: "50px" } },
        { key: "name", label: "Tên" },
        { key: "displayName", label: "Hiển thị" },
        { key: "level", label: "Level" },
        { key: "category", label: "Nhóm" },
        { key: "description", label: "Mô tả" },
        { key: "option", label: "Chức năng", thStyle: { width: "150px" } }
      ]
    };
  },

  computed: {
    categoryOptions() {
      return this.categories.map(c => ({
        value: c.id,
        text: c.name
      }));
    }
  },

  mounted() {
    this.loadCategories();
    this.loadData();
  },

  methods: {
    async loadData() {
      this.isBusy = true;
      try {
        const page = this.list.current_page - 1;
        const res = await axios.post(
          "/administrator/permissions/list",
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

    async loadCategories() {
      const res = await axios.post(
        "/administrator/permission-categories/list",
        null,
        { params: { page: 0, size: 100 } }
      );
      this.categories = res.data.content || [];
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
        level: 0,
        category: this.categories[0]?.id || null,
        description: ""
      };
      this.$refs.permissionModal.show();
    },

    editPermission(p) {
      this.form = {
        id: p.id,
        name: p.name,
        displayName: p.displayName,
        level: p.level,
        category: p.category?.id || p.category,
        description: p.description
      };
      this.$refs.permissionModal.show();
    },

    async savePermission() {
      await axios.post(
        "/administrator/permissions/saveOrUpdate",
        this.form
      );
      this.$refs.permissionModal.hide();
      this.loadData();
    },

    async deletePermission(p) {
      if (!confirm("Xóa quyền này?")) return;
      await axios.delete(`/administrator/permissions/${p.id}`);
      this.loadData();
    }
  }
};
</script>

<style scoped>
</style>
