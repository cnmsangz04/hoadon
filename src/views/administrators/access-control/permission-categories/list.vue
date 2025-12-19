<template>
  <div class="container-fluid py-3">
    <h4 class="mb-3 font-weight-bold">Danh sách nhóm quyền</h4>

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
              placeholder="Tìm theo tên nhóm"
              @keyup.enter="onFilter"
            />
          </b-form-group>
        </b-col>

        <b-col cols="6" class="text-right">
          <b-button size="sm" variant="primary" @click="onFilter">
            <i class="fas fa-search"></i> Tìm kiếm
          </b-button>
          <b-button
            size="sm"
            variant="success"
            class="ml-2"
            @click="showModal"
          >
            <i class="fas fa-plus"></i> Thêm nhóm
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

      <!-- STATUS -->
      <template #cell(status)="data">
        <b-badge :variant="data.item.status === 1 ? 'success' : 'secondary'">
          {{ data.item.status === 1 ? 'Hiển thị' : 'Ẩn' }}
        </b-badge>
      </template>

      <!-- ACTION -->
      <template #cell(option)="data">
        <b-button
          size="sm"
          variant="warning"
          class="mr-1"
          @click="editCategory(data.item)"
        >
          Sửa
        </b-button>
        <b-button
          size="sm"
          variant="danger"
          @click="deleteCategory(data.item)"
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
      ref="categoryModal"
      :title="form.id ? 'Sửa nhóm quyền' : 'Thêm nhóm quyền'"
      hide-footer
    >
      <b-form @submit.prevent="saveCategory">
        <b-form-group label="Tên nhóm">
          <b-form-input v-model="form.name" required />
        </b-form-group>

        <b-form-group label="Thứ tự hiển thị">
          <b-form-input
            type="number"
            v-model.number="form.orderIndex"
          />
        </b-form-group>

        <b-form-group label="Trạng thái">
          <b-form-select
            v-model.number="form.status"
            :options="statusOptions"
          />
        </b-form-group>

        <div class="text-right">
          <b-button type="submit" variant="primary">
            Lưu
          </b-button>
          <b-button
            variant="secondary"
            @click="$refs.categoryModal.hide()"
          >
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
  name: "PermissionCategoriesList",

  data() {
    return {
      keyword: "",
      items: [],
      isBusy: false,

      list: {
        current_page: 1,
        per_page: 10,
        total: 0
      },

      form: {
        id: null,
        name: "",
        orderIndex: 0,
        status: 1
      },

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
        const res = await axios.post(
          "/administrator/permission-categories/list",
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
      } catch (e) {
        console.error(e);
        this.items = [];
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

    showModal() {
      this.form = {
        id: null,
        name: "",
        orderIndex: 0,
        status: 1
      };
      this.$refs.categoryModal.show();
    },

    editCategory(c) {
      this.form = {
        id: c.id,
        name: c.name,
        orderIndex: c.orderIndex,
        status: c.status
      };
      this.$refs.categoryModal.show();
    },

    async saveCategory() {
      await axios.post(
        "/administrator/permission-categories/saveOrUpdate",
        this.form
      );
      this.$refs.categoryModal.hide();
      this.loadData();
    },

    async deleteCategory(c) {
      if (!confirm("Xóa nhóm quyền này?")) return;
      await axios.delete(
        `/administrator/permission-categories/${c.id}`
      );
      this.loadData();
    }
  }
};
</script>

<style scoped>
</style>
