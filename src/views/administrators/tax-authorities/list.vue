<template>
  <div class="container-fluid py-3">

    <!-- Title -->
    <h3 class="mb-3 font-weight-bold">Danh sách cơ quan thuế</h3>

    <!-- Filters Row -->
    <div class="d-flex flex-wrap mb-3 align-items-center">

      <!-- Search -->
      <b-input-group class="mr-2 mb-2" style="max-width: 260px;">
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

      <!-- Province select -->
      <b-form-select
        v-model="selectedProvince"
        :options="provinceOptions"
        class="mr-2 mb-2"
        style="max-width: 260px;"
      ></b-form-select>

      <!-- Button: danh sách cục thuế -->
      <b-button variant="danger" class="mr-2 mb-2">
        <i class="fa fa-list"></i> Danh sách Cục thuế Tỉnh/Thành
      </b-button>

      <!-- Button: thêm mới -->
      <b-button variant="success" class="mb-2">
        <i class="fa fa-plus"></i> Thêm mới
      </b-button>
    </div>

    <!-- Table -->
    <b-table
      bordered
      striped
      hover
      small
      :items="filteredItems"
      :fields="fields"
      responsive="sm"
    >

      <!-- Badge trạng thái -->
      <template #cell(status)="data">
        <b-badge variant="success" class="px-3 py-2">
          Hiển thị
        </b-badge>
      </template>

      <!-- Action menu -->
      <template #cell(actions)="data">
        <b-dropdown right variant="link" toggle-class="text-dark p-0">
          <template #button-content>
            <i class="fa fa-ellipsis-v"></i>
          </template>
          <b-dropdown-item @click="editItem(data.item)">
            Chỉnh sửa
          </b-dropdown-item>
          <b-dropdown-item @click="deleteItem(data.item)">
            Xóa
          </b-dropdown-item>
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
        { value: "uat", text: "Tổng cục thuế UAT" },
        { value: "hcm", text: "Cục Thuế TP Hồ Chí Minh" },
        { value: "hue", text: "Cục Thuế Thành phố Huế" }
      ],

      fields: [
        { key: "index", label: "#", sortable: false, class: "text-center" },
        { key: "code", label: "Mã cơ quan thuế" },
        { key: "province", label: "Cục thuế Tỉnh/ thành" },
        { key: "manager", label: "Cơ quan thuế quản lý" },
        { key: "status", label: "Trạng thái", class: "text-center" },
        { key: "created", label: "Ngày tạo" },
        { key: "actions", label: "Thao tác", class: "text-center" }
      ],

      items: [
        {
          index: 1,
          code: "97100",
          province: "Tổng cục thuế UAT",
          manager: "Tổng cục thuế quản lý UAT",
          created: "07/02/2025",
        },
        {
          index: 2,
          code: "70133",
          province: "Cục Thuế TP Hồ Chí Minh",
          manager: "Chi cục Thuế Quận Thủ Đức",
          created: "10/01/2022",
        },
        {
          index: 3,
          code: "70143",
          province: "Cục Thuế TP Hồ Chí Minh",
          manager: "Chi cục Thuế Huyện Cần Giờ",
          created: "04/12/2021",
        },
        {
          index: 4,
          code: "70141",
          province: "Cục Thuế TP Hồ Chí Minh",
          manager:
            "Huyện Nhà Bè - Chi cục Thuế khu vực Quận 7 - Nhà Bè",
          created: "04/12/2021",
        },
        {
          index: 5,
          code: "70139",
          province: "Cục Thuế TP Hồ Chí Minh",
          manager: "Chi cục Thuế Huyện Bình Chánh",
          created: "04/12/2021",
        },
        {
          index: 6,
          code: "70137",
          province: "Cục Thuế TP Hồ Chí Minh",
          manager:
            "Huyện Hóc Môn - Chi cục Thuế khu vực Quận 12 - huyện Hóc Môn",
          created: "04/12/2021",
        },
        {
          index: 7,
          code: "70135",
          province: "Cục Thuế TP Hồ Chí Minh",
          manager: "Chi cục Thuế Huyện Củ Chi",
          created: "04/12/2021",
        },
        {
          index: 8,
          code: "70134",
          province: "Cục Thuế TP Hồ Chí Minh",
          manager: "Chi cục Thuế Quận Bình Tân",
          created: "04/12/2021",
        },
        {
          index: 9,
          code: "70145",
          province: "Cục Thuế TP Hồ Chí Minh",
          manager: "Chi cục Thuế Thành phố Thủ Đức",
          created: "04/12/2021",
        }
      ]
    };
  },

  computed: {
    filteredItems() {
      let data = this.items;

      // Search filter
      if (this.searchKeyword) {
        const key = this.searchKeyword.toLowerCase();
        data = data.filter(item =>
          item.code.toLowerCase().includes(key) ||
          item.province.toLowerCase().includes(key) ||
          item.manager.toLowerCase().includes(key)
        );
      }

      // Province dropdown filter
      if (this.selectedProvince) {
        data = data.filter(item =>
          item.province.toLowerCase().includes(this.selectedProvince)
        );
      }

      return data;
    }
  },

  methods: {
    async editItem(item) {
      alert("Chỉnh sửa: " + item.code);
	  
	  const res = await axios.post("/auth/login", {
	           username: this.username,
	           password: this.password
	         });
    },
    deleteItem(item) {
      alert("Xóa: " + item.code);
    }
  }
};
</script>

<style scoped>
.badge {
  font-size: 14px;
}
</style>
