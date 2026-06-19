<template>
    <div class="container-fluid py-3 product-list">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <h4 class="mb-0 font-weight-bold">
                Danh mục hàng hóa
            </h4>
            <div>
                <b-button
                    size="sm"
                    variant="outline-primary"
                    class="mr-2"
                    @click="reload"
                >
                    <i class="fas fa-sync-alt"></i> Làm mới
                </b-button>
                <b-button size="sm" variant="outline-success" class="mr-2" to="/imports/product">
                    <i class="fas fa-box"></i> Import sản phẩm
                </b-button>
                <b-button size="sm" variant="success" @click="openCreate">
                    <i class="fas fa-plus"></i> Thêm hàng hóa
                </b-button>
            </div>
        </div>

        <b-card class="mb-3 shadow-sm border-0">
            <b-row>
                <b-col md="5" class="mb-2">
                    <b-input-group>
                        <b-input-group-prepend is-text>
                            <i class="fas fa-search"></i>
                        </b-input-group-prepend>
                        <b-form-input
                            v-model.trim="filters.keyword"
                            placeholder="Tìm theo Mã hoặc Tên sản phẩm..."
                            @keyup.enter="applyFilters"
                        />
                    </b-input-group>
                </b-col>
                <b-col md="4" class="mb-2">
                    <b-form-select
                        v-model="filters.status"
                        :options="statusOptions"
                    >
                        <template #first>
                            <b-form-select-option :value="null"
                                >Tất cả trạng thái</b-form-select-option
                            >
                        </template>
                    </b-form-select>
                </b-col>
                <b-col md="3" class="text-right">
                    <b-button variant="primary" block @click="applyFilters"
                        >Tìm kiếm</b-button
                    >
                </b-col>
            </b-row>
        </b-card>

        <b-card class="shadow-sm border-0">
            <b-table
                bordered
                hover
                small
                show-empty
                :items="list.data"
                :fields="fields"
                :busy="isBusy"
                empty-text="Không tìm thấy sản phẩm nào"
            >
                <template #table-busy>
                    <div class="text-center text-primary my-2">
                        <b-spinner class="align-middle"></b-spinner>
                        <strong> Đang tải dữ liệu...</strong>
                    </div>
                </template>

                <template #cell(index)="{ index }">
                    {{ index + 1 + (list.current_page - 1) * list.per_page }}
                </template>

                <template #cell(code)="data">
                    <span class="font-weight-bold text-primary">{{
                        data.item.code
                    }}</span>
                </template>

                <template #cell(company)="data">
                    <div class="font-weight-bold text-dark">
                        {{ currentCompanyName || "---" }}
                    </div>
                </template>

                <template #cell(price)="data">
                    <span class="text-danger font-weight-bold">{{
                        formatMoney(data.item.price)
                    }}</span>
                </template>

                <template #cell(vatRate)="data">
                    <b-badge variant="light">
                        {{
                            vatNameById[data.item.vatRate] ||
                            (data.item.vatRate === -1
                                ? "KCT"
                                : data.item.vatRate + "%")
                        }}
                    </b-badge>
                </template>

                <template #cell(status)="data">
                    <b-badge
                        :variant="
                            data.item.status === 1 ? 'success' : 'secondary'
                        "
                    >
                        {{
                            data.item.status === 1
                                ? "Đang bán"
                                : "Ngừng kinh doanh"
                        }}
                    </b-badge>
                </template>

                <template #cell(option)="{ item }">
                    <b-dropdown
                        size="sm"
                        right
                        variant="link"
                        toggle-class="text-decoration-none"
                        no-caret
                        boundary="window"
                    >
                        <template #button-content>
                            <i class="fas fa-ellipsis-h text-muted"></i>
                        </template>
                        <b-dropdown-item @click="openEdit(item)">
                            <i class="fas fa-edit mr-2"></i>Cập nhật
                        </b-dropdown-item>
                        <b-dropdown-item @click="toggleLock(item)">
                            <i
                                class="fas mr-2"
                                :class="
                                    item.status == 1
                                        ? 'fa-lock text-warning'
                                        : 'fa-unlock text-success'
                                "
                            ></i>
                            {{ item.status == 1 ? "Khóa" : "Mở khóa" }}
                        </b-dropdown-item>
                        <b-dropdown-item class="text-danger" @click="deleteItem(item)">
                            <i class="fas fa-trash-alt mr-2"></i>Xóa
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
            ref="productModal"
            :title="form.id ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm mới'"
            ok-title="Lưu dữ liệu"
            cancel-title="Đóng"
            @ok.prevent="saveData"
            :busy="isSaving"
            size="lg"
        >
            <b-form>
                <b-row>
                    <b-col md="4">
                        <b-form-group label="Mã sản phẩm" :state="state('code')">
                            <b-form-input
                                v-model.trim="form.code"
                                placeholder="Ví dụ: SP001"
                                :state="state('code')"
                                required
                            />
                            <b-form-invalid-feedback :state="state('code')">{{ errors.code }}</b-form-invalid-feedback>
                        </b-form-group>
                    </b-col>
                    <b-col md="8">
                        <b-form-group label="Tên sản phẩm" :state="state('name')">
                            <b-form-input
                                v-model.trim="form.name"
                                placeholder="Nhập tên sản phẩm"
                                :state="state('name')"
                                required
                            />
                            <b-form-invalid-feedback :state="state('name')">{{ errors.name }}</b-form-invalid-feedback>
                        </b-form-group>
                    </b-col>

                    <b-col md="12">
                        <b-form-group label="Thuộc Công ty">
                            <b-form-input
                                :value="
                                    currentCompanyName ||
                                    'Đang tải thông tin...'
                                "
                                readonly
                                class="bg-light font-weight-bold text-dark"
                            />
                        </b-form-group>
                    </b-col>
                </b-row>

                <b-row>
                    <b-col md="4">
                        <b-form-group label="Đơn vị tính">
                            <b-form-input v-model="form.unit" />
                        </b-form-group>
                    </b-col>
                    <b-col md="4">
                        <b-form-group label="Đơn giá" :state="state('price')">
                            <b-form-input
                                type="number"
                                v-model.number="form.price"
                                min="0"
                                :state="state('price')"
                            />
                            <b-form-invalid-feedback :state="state('price')">{{ errors.price }}</b-form-invalid-feedback>
                        </b-form-group>
                    </b-col>
                    <b-col md="4">
                        <b-form-group label="Thuế suất">
                            <b-form-select
                                v-model.number="form.vatRate"
                                :options="vatOptions"
                            />
                        </b-form-group>
                    </b-col>
                </b-row>

                <b-form-group label="Mô tả">
                    <b-form-textarea
                        v-model="form.description"
                        rows="3"
                    ></b-form-textarea>
                </b-form-group>

                <b-form-group label="Trạng thái">
                    <b-form-checkbox
                        v-model="form.status"
                        :value="1"
                        :unchecked-value="0"
                        switch
                    >
                        {{
                            form.status === 1
                                ? "Đang kinh doanh"
                                : "Ngừng kinh doanh"
                        }}
                    </b-form-checkbox>
                </b-form-group>
            </b-form>
        </b-modal>
    </div>
</template>

<script>
import axios from "@/plugins/axios";
import { pageFrom, pageItems, pageTo, pageTotal } from "@/utils/pagination";
import { firstError, hasErrors, numberRange, required } from "@/utils/validators";
import PaginationBar from "@/views/components/pagination_bar.vue";

export default {
    name: "ProductList",
    components: { PaginationBar },
    data() {
        return {
            isBusy: false,
            isSaving: false,
            currentCompanyName: "", // Biến chứa tên công ty của User đăng nhập
            list: {
                data: [],
                total: 0,
                per_page: 10,
                current_page: 1,
                from: 0,
                to: 0,
            },
            pageSizes: [10, 20, 50, 100],
            vatOptions: [],
            vatNameById: {},
            filters: { keyword: "", status: null },
            statusOptions: [
                { value: 1, text: "Đang bán" },
                { value: 0, text: "Đã khóa" },
            ],
            // Cấu hình các cột của bảng
            fields: [
                {
                    key: "index",
                    label: "#",
                    thClass: "text-center",
                    tdClass: "text-center",
                    width: "50px",
                },
                { key: "code", label: "Mã hàng", sortable: true },
                { key: "name", label: "Tên hàng hóa", sortable: true },
                { key: "company", label: "Tên công ty", sortable: true }, // Hiển thị tên công ty
                { key: "unit", label: "ĐVT", tdClass: "text-center" },
                { key: "price", label: "Đơn giá", tdClass: "text-right" },
                { key: "vatRate", label: "Thuế", tdClass: "text-center" },
                { key: "status", label: "Trạng thái", tdClass: "text-center" },
                { key: "option", label: "Thao tác", tdClass: "text-center" },
            ],
            form: {
                id: null,
                code: "",
                name: "",
                price: 0,
                unit: "",
                vatRate: -1,
                description: "",
                status: 1,
            },
            errors: {},
        };
    },
    mounted() {
        this.loadProfile(); // Lấy thông tin User & Công ty ngay khi load trang
        this.loadVatOptions();
        this.loadData();
    },
    methods: {
        // 1. Lấy tên công ty hiện tại từ thông tin đăng nhập
        async loadProfile() {
            try {
                const appCompany = this.$app?.info?.company || {};
                if (appCompany.name) {
                    this.currentCompanyName = appCompany.name;
                    return;
                }
                const res = await axios.get("/auth/info", {
                    meta: { suppressGlobalErrorToast: true },
                });
                const company = res.data?.company || {};
                this.currentCompanyName = company.name || "";
            } catch (e) {
                this.currentCompanyName = "";
            }
        },

        // 2. Tải danh sách sản phẩm
        async loadData() {
            this.isBusy = true;
            try {
                const res = await axios.post(
                    "/categories/product/list",
                    this.filters,
                    {
                        params: {
                            page: this.list.current_page - 1,
                            size: this.list.per_page,
                        },
                    }
                );
                const d = res.data;

                // Xử lý dữ liệu trả về từ Page (Spring Boot)
                this.list.data = pageItems(d);
                this.list.total = pageTotal(d);

                // Tính toán hiển thị "Từ dòng X đến Y"
                this.list.from = pageFrom(d, this.list.current_page, this.list.per_page);
                const numberOfElements = Array.isArray(this.list.data)
                    ? this.list.data.length
                    : 0;
                this.list.to = pageTo(d, numberOfElements, this.list.current_page, this.list.per_page);
            } catch (e) {
                if (e?.response?.status === 403) return;
                this.$toastr && this.$toastr.error(e?.response?.data?.message || "Lỗi tải danh mục sản phẩm", "Lỗi");
            } finally {
                this.isBusy = false;
            }
        },

        // 3. Tải danh sách thuế suất
        async loadVatOptions() {
            try {
                const res = await axios.get("/categories/product/vat-rates");
                const items = Array.isArray(res.data) ? res.data : [];
                // Tạo tùy chọn cho ô chọn và ánh xạ để hiển thị
                this.vatOptions = items.map(it => ({ value: it.code, text: it.label }));
                this.vatNameById = items.reduce((acc, it) => {
                    acc[it.code] = it.label;
                    return acc;
                }, {});
            } catch (e) {
                console.error("Lỗi load thuế:", e);
                this.$toastr && this.$toastr.warning("Không thể tải thuế suất", "Cảnh báo");
            }
        },

        // Các hàm xử lý sự kiện
        applyFilters() {
            this.list.current_page = 1;
            this.loadData();
        },
        onPageChange(page) {
            this.list.current_page = page;
            this.loadData();
        },
        reload() {
            this.filters = { keyword: "", status: null };
            this.list.current_page = 1;
            this.loadData();
        },
        onPageSizeChange(size) {
            this.list.per_page = size;
            this.list.current_page = 1;
            this.loadData();
        },
        formatMoney(val) {
            return new Intl.NumberFormat("vi-VN", {
                style: "currency",
                currency: "VND",
            }).format(val);
        },

        // Mở Modal Thêm mới
        openCreate() {
            this.form = {
                id: null,
                code: "",
                name: "",
                price: 0,
                unit: "",
                vatRate: -1,
                description: "",
                status: 1,
            };
            this.errors = {};
            // Không cần gán companyId vì phía backend tự lấy
            this.$refs.productModal.show();
        },

        // Mở Modal Cập nhật
        openEdit(item) {
            this.form = { ...item };
            this.errors = {};
            this.$refs.productModal.show();
        },

        state(field) {
            return this.errors[field] ? false : null;
        },

        validateForm() {
            this.errors = {
                code: required(this.form.code, "Vui lòng nhập mã sản phẩm"),
                name: required(this.form.name, "Vui lòng nhập tên sản phẩm"),
                price: firstError([
                    required(this.form.price, "Vui lòng nhập đơn giá"),
                    numberRange(this.form.price, 0, null, "Đơn giá không được âm"),
                ]),
            };
            Object.keys(this.errors).forEach((key) => {
                if (!this.errors[key]) delete this.errors[key];
            });
            return !hasErrors(this.errors);
        },

        // Lưu dữ liệu
        async saveData() {
            if (!this.validateForm()) {
                this.$toastr && this.$toastr.warning(firstError(Object.values(this.errors)) || "Vui lòng kiểm tra lại thông tin", "Cảnh báo");
                return;
            }
            this.isSaving = true;
            try {
                // Gửi dữ liệu lên Server.
                // Lưu ý: không gửi companyId; nếu có gửi thì phía backend cũng ghi đè bằng ID của user đang đăng nhập.
                await axios.post("/categories/product/save", this.form, {
                    meta: { suppressGlobalErrorToast: true },
                });

                this.$toastr && this.$toastr.success("Lưu sản phẩm thành công", "Thành công");
                this.$refs.productModal.hide();
                this.loadData();
            } catch (e) {
                this.$toastr && this.$toastr.error(e.response?.data?.message || "Lỗi khi lưu dữ liệu", "Lỗi");
            } finally {
                this.isSaving = false;
            }
        },

        // Khóa/Mở khóa nhanh
        async toggleLock(item) {
            try {
                const newStatus = item.status === 1 ? 0 : 1;
                await axios.post("/categories/product/save", {
                    ...item,
                    status: newStatus,
                }, {
                    meta: { suppressGlobalErrorToast: true },
                });
                this.$toastr && this.$toastr.success("Cập nhật trạng thái thành công", "Thành công");
                this.loadData();
            } catch (e) {
                this.$toastr && this.$toastr.error("Không thể thay đổi trạng thái", "Lỗi");
            }
        },
        async deleteItem(item) {
            if (!item || !item.id) return;
            const label = item.name || item.code || item.id;
            if (!window.confirm(`Xóa sản phẩm "${label}"?`)) return;
            try {
                await axios.delete(`/categories/product/${item.id}`, {
                    meta: { suppressGlobalErrorToast: true },
                });
                this.$toastr && this.$toastr.success("Xóa sản phẩm thành công", "Thành công");
                this.loadData();
            } catch (e) {
                this.$toastr && this.$toastr.error(e.response?.data?.message || "Xóa sản phẩm thất bại", "Lỗi");
            }
        },
    },
};
</script>

<style scoped>
.product-list .card {
    border-radius: 10px;
}
.text-right {
    text-align: right;
}
/* Kiểu cho ô input chỉ đọc nhìn đẹp hơn */
.bg-light {
    background-color: #f8f9fa !important;
}
</style>
