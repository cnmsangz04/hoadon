<template>
  <div class="container-fluid py-3 permissions">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Danh sách quyền</h4>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="onFilter">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="success" @click="showModal">
          <i class="fas fa-plus"></i>
          Thêm quyền
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
            <b-form-input
              v-model="keyword"
              placeholder="Tìm theo tên / hiển thị"
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
        <!-- STT -->
        <template #cell(index)="data">
          {{ data.index + 1 + (list.current_page - 1) * list.per_page }}
        </template>

        <template #cell(category)="data">
          <b-badge pill variant="light" class="category-pill">
            {{ categoryName(data.item) }}
          </b-badge>
        </template>

        <template #cell(level)="data">
          <b-badge variant="info">Level {{ data.item.level }}</b-badge>
        </template>

        <template #cell(status)="data">
          <b-badge :variant="data.item.status === 1 ? 'success' : 'secondary'">
            {{ data.item.status === 1 ? 'Hiển thị' : 'Ẩn' }}
          </b-badge>
        </template>

        <template #cell(option)="data">
          <b-dropdown
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
            <b-dropdown-item
              class="text-center"
              href="#"
              @click.prevent="editPermission(data.item)"
            >
              Cập nhật
            </b-dropdown-item>
            <b-dropdown-item
              v-if="data.item.status === 1"
              class="text-center"
              href="#"
              @click.prevent="hidePermission(data.item)"
            >
              <span class="text-warning">Ẩn</span>
            </b-dropdown-item>
            <b-dropdown-item
              v-else
              class="text-center"
              href="#"
              @click.prevent="showPermission(data.item)"
            >
              <span class="text-success">Hiện</span>
            </b-dropdown-item>
          </b-dropdown>
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

    <b-modal
      ref="permissionModal"
      :title="form.id ? 'Cập nhật quyền' : 'Thêm quyền'"
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
          <small v-if="!validLevel" class="text-danger">Level phải từ 0 đến 2</small>
        </b-form-group>

        <b-form-group label="Nhóm quyền">
          <b-form-select
            v-model="form.category"
            :options="categoryOptions"
            required
          />
          <small v-if="!form.category" class="text-danger">Vui lòng chọn nhóm</small>
        </b-form-group>

        <b-form-group label="Mô tả">
          <b-form-textarea v-model="form.description" rows="2" />
        </b-form-group>

        <b-form-group label="Trạng thái">
          <b-form-select
            v-model.number="form.status"
            :options="statusOptions"
          />
        </b-form-group>

        <div class="text-right">
          <b-button type="submit" :disabled="!canSubmit" variant="primary">Lưu</b-button>
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
      list: { current_page: 1, per_page: 10, total: 0 },
      form: {
        id: null,
        name: "",
        displayName: "",
        level: 0,
        category: null,
        description: "",
        status: 1
      },
      statusOptions: [
        { value: 1, text: "Hiển thị" },
        { value: 0, text: "Ẩn" }
      ],
      fields: [
        { key: "index", label: "#", thStyle: { width: "50px" } },
        { key: "name", label: "Tên" },
        { key: "displayName", label: "Hiển thị" },
        { key: "level", label: "Level" },
        { key: "category", label: "Nhóm" },
        { key: "status", label: "Trạng thái", thStyle: { width: "120px" } },
        { key: "description", label: "Mô tả" },
        { key: "option", label: "Chức năng", thStyle: { width: "80px" } }
      ]
    };
  },
  computed: {
    categoryOptions() {
      // Only show visible categories (status === 1)
      return this.categories
        .filter(c => c.status === 1)
        .map(c => ({ value: c.id, text: c.name }));
    },
    categoryById() {
      const map = {};
      for (const c of this.categories) map[c.id] = c;
      return map;
    },
    validLevel() {
      const lvl = Number(this.form.level);
      return Number.isFinite(lvl) && lvl >= 0 && lvl <= 2;
    },
    canSubmit() {
      return (
        !!this.form.name &&
        !!this.form.displayName &&
        this.validLevel &&
        !!this.form.category &&
        (this.form.status === 0 || this.form.status === 1)
      );
    }
  },
  mounted() {
    this.loadCategories();
    this.loadData();
  },
  methods: {
    // Return category display name robustly using object name or id lookup
    categoryName(item) {
      const cat = item?.category;
      if (!cat && cat !== 0) return 'Khác';
      if (typeof cat === 'object') {
        if (cat.name && String(cat.name).trim()) return cat.name;
        if (cat.id != null && this.categoryById[cat.id]?.name) {
          return this.categoryById[cat.id].name;
        }
        return 'Khác';
      }
      // cat is likely an id (number/string)
      const name = this.categoryById[cat]?.name;
      return (name && String(name).trim()) ? name : 'Khác';
    },
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
      const cats = res.data.content || [];
      // Sort by orderIndex (sothutu) ascending
      this.categories = cats.sort((a, b) => {
        const aOrder = Number(a.orderIndex ?? a.sothutu ?? 999);
        const bOrder = Number(b.orderIndex ?? b.sothutu ?? 999);
        return aOrder - bOrder;
      });
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
        category: this.categoryOptions[0]?.value || null,
        description: "",
        status: 1
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
        description: p.description,
        status: p.status
      };
      this.$refs.permissionModal.show();
    },
    async savePermission() {
      if (!this.canSubmit) return;
      try {
        await axios.post(
          "/administrator/permissions/saveOrUpdate",
          this.form
        );
        // Toast th�nh c�ng
        this.$toastr.success(this.form.id ? 'Cập nhật quyền thành công' : 'Thêm quyền thành công');
        this.$refs.permissionModal.hide();
        this.loadData();
      } catch (e) {
        // Interceptor Axios d� hi?n th? toast l?i
        console.error(e);
      }
    },
    async hidePermission(p) {
      try {
        await axios.post(
          `/administrator/permissions/${p.id}/status`,
          null,
          { params: { status: 0 } }
        );
        this.$toastr.success('Đã ẩn quyền');
        this.loadData();
      } catch (e) {
        console.error(e);
      }
    },
    async showPermission(p) {
      try {
        await axios.post(
          `/administrator/permissions/${p.id}/status`,
          null,
          { params: { status: 1 } }
        );
        this.$toastr.success('Đã hiện quyền');
        this.loadData();
      } catch (e) {
        console.error(e);
      }
    }
  }
};
</script>

<style scoped>
.permissions .card.shadow-sm { border-radius: 10px; }
.permissions .table-hover tbody tr:hover { background-color: #fafbfd; }
.permissions .btn-outline-primary { border-color: #dfe7ff; }
.permissions .btn-outline-primary:hover { background: #eef3ff; }

.permissions .table thead th { background: #f7f9fc; border-bottom: 1px solid #ecf0f6; color: #4a5568; font-weight: 700; }
</style>