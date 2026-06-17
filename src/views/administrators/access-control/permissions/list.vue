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
      <b-row class="align-items-end">
        <b-col lg="4" md="6" class="mb-2">
          <label class="filter-label">Từ khóa</label>
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input
              v-model.trim="filters.keyword"
              placeholder="Tìm theo tên / hiển thị"
              @keyup.enter="onFilter"
            />
          </b-input-group>
        </b-col>
        <b-col lg="3" md="6" class="mb-2">
          <label class="filter-label">Nhóm quyền</label>
          <b-form-select
            v-model="filters.categoryId"
            :options="filterCategoryOptions"
          />
        </b-col>
        <b-col lg="2" md="4" class="mb-2">
          <label class="filter-label">Level</label>
          <b-form-select
            v-model="filters.level"
            :options="levelFilterOptions"
          />
        </b-col>
        <b-col lg="2" md="4" class="mb-2">
          <label class="filter-label">Trạng thái</label>
          <b-form-select
            v-model="filters.status"
            :options="statusFilterOptions"
          />
        </b-col>
        <b-col lg="1" md="4" class="mb-2 text-right">
          <b-button size="sm" variant="primary" class="filter-action" @click="onFilter">
            Lọc
          </b-button>
          <b-button size="sm" variant="light" class="filter-action mt-1" @click="resetFilter">
            Xóa
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

      <pagination-bar
        :current.sync="list.current_page"
        :size.sync="list.per_page"
        :total="list.total"
        :sizes="pageSizes"
        @page-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </b-card>

    <b-modal
      ref="permissionModal"
      :title="form.id ? 'Cập nhật quyền' : 'Thêm quyền'"
      hide-footer
    >
      <b-form novalidate @submit.prevent="savePermission">
        <b-form-group label="Tên quyền" :state="state('name')">
          <b-form-input v-model.trim="form.name" required :state="state('name')" />
          <b-form-invalid-feedback :state="state('name')">
            {{ invalidFeedback('name') }}
          </b-form-invalid-feedback>
        </b-form-group>

        <b-form-group label="Tên hiển thị" :state="state('displayName')">
          <b-form-input v-model.trim="form.displayName" required :state="state('displayName')" />
          <b-form-invalid-feedback :state="state('displayName')">
            {{ invalidFeedback('displayName') }}
          </b-form-invalid-feedback>
        </b-form-group>

        <b-form-group label="Level" :state="state('level')">
          <b-form-input
            type="number"
            min="0"
            max="2"
            v-model.number="form.level"
            :state="state('level')"
          />
          <b-form-invalid-feedback :state="state('level')">
            {{ invalidFeedback('level') }}
          </b-form-invalid-feedback>
        </b-form-group>

        <b-form-group label="Nhóm quyền" :state="state('category')">
          <b-form-select
            v-model="form.category"
            :options="categoryOptions"
            required
            :state="state('category')"
          />
          <b-form-invalid-feedback :state="state('category')">
            {{ invalidFeedback('category') }}
          </b-form-invalid-feedback>
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
import { pageItems, pageTotal } from "@/utils/pagination";
import PaginationBar from "@/views/components/pagination_bar.vue";

export default {
  name: "PermissionsList",
  components: { PaginationBar },
  data() {
    return {
      filters: {
        keyword: "",
        categoryId: null,
        level: null,
        status: null
      },
      items: [],
      categories: [],
      isBusy: false,
      list: { current_page: 1, per_page: 10, total: 0 },
      pageSizes: [10, 20, 50, 100],
      form: {
        id: null,
        name: "",
        displayName: "",
        level: 0,
        category: null,
        description: "",
        status: 1
      },
      errors: {},
      statusOptions: [
        { value: 1, text: "Hiển thị" },
        { value: 0, text: "Ẩn" }
      ],
      levelFilterOptions: [
        { value: null, text: "Tất cả" },
        { value: 0, text: "Level 0" },
        { value: 1, text: "Level 1" },
        { value: 2, text: "Level 2" }
      ],
      statusFilterOptions: [
        { value: null, text: "Tất cả" },
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
      // Chỉ hiển thị nhóm quyền đang bật (status === 1)
      return this.categories
        .filter(c => c.status === 1)
        .map(c => ({ value: c.id, text: c.name }));
    },
    filterCategoryOptions() {
      return [
        { value: null, text: "Tất cả" },
        ...this.categories.map(c => ({ value: c.id, text: c.name }))
      ];
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
    // Trả về tên nhóm quyền bằng tên object hoặc tra cứu theo id
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
              keyword: this.filters.keyword || undefined,
              categoryId: this.filters.categoryId ?? undefined,
              level: this.filters.level ?? undefined,
              status: this.filters.status ?? undefined,
              page,
              size: this.list.per_page
            }
          }
        );
        this.items = pageItems(res.data);
        this.list.total = pageTotal(res.data);
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
      // Sắp xếp tăng dần theo orderIndex (sothutu)
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
    resetFilter() {
      this.filters = {
        keyword: "",
        categoryId: null,
        level: null,
        status: null
      };
      this.onFilter();
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
      this.errors = {};
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
      this.errors = {};
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
      if (!this.validateForm()) return;
      try {
        await axios.post(
          "/administrator/permissions/saveOrUpdate",
          this.form
        );
        // Toast thành công
        this.$toastr.success(this.form.id ? 'Cập nhật quyền thành công' : 'Thêm quyền thành công');
        this.$refs.permissionModal.hide();
        this.loadData();
      } catch (e) {
        // Interceptor Axios đã hiển thị toast lỗi
        console.error(e);
      }
    },
    validateForm() {
      const errors = {};
      if (!String(this.form.name || "").trim()) {
        errors.name = ["Vui lòng nhập tên quyền"];
      }
      if (!String(this.form.displayName || "").trim()) {
        errors.displayName = ["Vui lòng nhập tên hiển thị"];
      }
      if (!this.validLevel) {
        errors.level = ["Level phải từ 0 đến 2"];
      }
      if (!this.form.category) {
        errors.category = ["Vui lòng chọn nhóm quyền"];
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
.permissions .filter-label {
  display: block;
  margin-bottom: 6px;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}
.permissions .filter-action {
  width: 100%;
}

.permissions .table thead th { background: #f7f9fc; border-bottom: 1px solid #ecf0f6; color: #4a5568; font-weight: 700; }
</style>
