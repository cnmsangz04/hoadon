<template>
  <div class="container-fluid py-3">

    <!-- Tiêu đề -->
    <h3 class="mb-3">Danh sách cơ quan thuế</h3>

    <!-- Bộ lọc -->
    <div class="d-flex flex-wrap mb-3 align-items-center">

      <!-- Ô tìm kiếm -->
      <b-input-group class="mr-3 mb-2" style="max-width: 260px;">
        <b-form-input
          v-model="searchKeyword"
          placeholder="Nhập từ khóa tìm kiếm..."
        ></b-form-input>
        <b-input-group-append>
          <b-button variant="outline-secondary">
            <i class="fa fa-search"></i>
          </b-button>
        </b-input-group-append>
      </b-input-group>

      <!-- Dropdown cục thuế -->
      <b-form-select
        v-model="selectedProvince"
        :options="provinceOptions"
        class="mr-3 mb-2"
        style="max-width: 260px;"
      >
      </b-form-select>

      <!-- Nút danh sách cục thuế tỉnh/thành -->
      <b-button variant="danger" class="mr-3 mb-2">
        <i class="fa fa-list"></i> Danh sách Cục thuế Tỉnh/Thành
      </b-button>

      <!-- Nút thêm mới -->
      <b-button variant="success" class="mb-2">
        <i class="fa fa-plus"></i> Thêm mới
      </b-button>
    </div>

    <!-- Bảng danh sách -->
    <b-table
      bordered
      striped
      hover
      :items="filteredItems"
      :fields="fields"
      small
    >
      <!-- Trạng thái -->
      <template #cell(status)="data">
        <b-badge variant="success" class="px-3 py-2">Hiển thị</b-badge>
      </template>

      <!-- Thao tác -->
      <template #cell(actions)="data">
        <b-dropdown right variant="link" toggle-class="text-dark p-0">
          <template #button-content>
            <i class="fa fa-ellipsis-v"></i>
          </template>
          <b-dropdown-item>Cập nhật</b-dropdown-item>
          <b-dropdown-item>Xóa</b-dropdown-item>
        </b-dropdown>
      </template>
    </b-table>

  </div>
</template>

<script>
export default {
  data() {
    return {
      searchKeyword: "",
      selectedProvince: null,

      provinceOptions: [
        { value: null, text: "Chọn cục thuế Tỉnh/Thành" },
        { value: 1, text: "Tổng cục thuế UAT" },
        { value: 2, text: "Thuế Thành phố Huế" },
        { value: 3, text: "Thuế TP Hồ Chí Minh" }
      ],

      fields: [
        { key: "index", label: "#", sortable: false },
        { key: "code", label: "Mã cơ quan thuế" },
        { key: "province", label: "Cục thuế Tỉnh/Thành" },
        { key: "manager", label: "Cơ quan thuế quản lý" },
        { key: "status", label: "Trạng thái" },
        { key: "created", label: "Ngày tạo" },
        { key: "actions", label: "Thao tác" }
      ],

      items: [
        {
          index: 1,
          code: "97100",
          province: "Tổng cục thuế UAT",
          manager: "Tổng cục thuế quản lý UAT",
          created: "14/06/2025",
        },
        {
          index: 2,
          code: "41119",
          province: "Thuế Thành phố Huế",
          manager: "Quận Phú Xuân - Thuế cơ sở 1 Thành phố Huế",
          created: "20/03/2025",
        },
        {
          index: 3,
          code: "70143",
          province: "Thuế Thành phố Hồ Chí Minh",
          manager: "Thuế cơ sở 20 TP Hồ Chí Minh",
          created: "04/12/2021",
        },
        {
          index: 4,
          code: "70141",
          province: "Thuế Thành phố Hồ Chí Minh",
          manager: "Xã Nhà Bè - Thuế cơ sở 7 TP Hồ Chí Minh",
          created: "04/12/2021",
        }
      ]
    };
  },

  computed: {
    filteredItems() {
      const keyword = this.searchKeyword.toLowerCase();
      return this.items.filter(item => 
        item.code.toLowerCase().includes(keyword) ||
        item.province.toLowerCase().includes(keyword) ||
        item.manager.toLowerCase().includes(keyword)
      );
    }
  }
};
</script>

<style scoped>
.badge {
  font-size: 14px;
}
</style>