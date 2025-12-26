<template>
    <div class="container-fluid py-3">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <h3 class="m-0 font-weight-bold text-primary">Danh sách cơ quan thuế</h3>
            <b-button variant="success" @click="openModal(null)" class="shadow-sm">
                <b-icon icon="plus-circle-fill" class="mr-1"></b-icon> Thêm mới
            </b-button>
        </div>

        <div class="d-flex flex-wrap mb-3 align-items-center bg-light p-3 rounded border">
            <b-input-group class="mr-3" style="max-width: 350px">
                <b-form-input v-model="filter.keyword" @debounce="fetchData"
                    placeholder="Tìm kiếm theo mã, tên..."></b-form-input>
                <b-input-group-append>
                    <b-button variant="outline-primary" @click="fetchData">
                        <b-icon icon="search"></b-icon>
                    </b-button>
                </b-input-group-append>
            </b-input-group>

            <b-button variant="outline-secondary" @click="fetchData" title="Tải lại dữ liệu">
                <b-icon icon="arrow-clockwise"></b-icon> Tải lại
            </b-button>
        </div>

        <b-table bordered striped hover responsive :items="items" :fields="fields" :busy="loading" show-empty
            empty-text="Không tìm thấy dữ liệu phù hợp" head-variant="light">
            <template #table-busy>
                <div class="text-center text-primary my-4">
                    <b-spinner class="align-middle mr-2"></b-spinner>
                    <strong>Đang tải dữ liệu...</strong>
                </div>
            </template>

            <template #cell(managerName)="data">
                <span v-if="data.item.parent" class="text-info">
                    <b-icon icon="arrow-return-right" font-scale="0.9"></b-icon>
                    {{ data.item.parent.name }}
                </span>
                <span v-else class="text-muted font-italic small">-- Cấp cao nhất --</span>
            </template>

            <template #cell(status)="data">
                <b-badge :variant="data.item.status === 1 ? 'success' : 'secondary'" pill>
                    {{ data.item.status === 1 ? "Đang hoạt động" : "Đã ẩn" }}
                </b-badge>
            </template>

            <template #cell(created)="data">
                {{ formatDate(data.item.createdAt) }}
            </template>

            <template #cell(actions)="data">
                <div class="d-flex justify-content-center">
                    <b-button size="sm" variant="outline-primary" class="mr-2" @click="openModal(data.item)"
                        title="Chỉnh sửa">
                        <b-icon icon="pencil-square"></b-icon>
                    </b-button>
                    <b-button size="sm" variant="outline-danger" @click="deleteItem(data.item)" title="Xóa">
                        <b-icon icon="trash"></b-icon>
                    </b-button>
                </div>
            </template>
        </b-table>

        <div class="d-flex justify-content-between align-items-center mt-3" v-if="totalRows > 0">
            <div class="text-muted small">
                Hiển thị từ {{ (currentPage - 1) * perPage + 1 }} đến
                {{ Math.min(currentPage * perPage, totalRows) }} trong tổng số {{ totalRows }} bản ghi
            </div>
            <b-pagination v-model="currentPage" :total-rows="totalRows" :per-page="perPage" @change="handlePageChange"
                align="right" class="mb-0"></b-pagination>
        </div>

        <b-modal v-model="showModal" :title="isEditMode ? 'Cập nhật thông tin' : 'Thêm mới Cơ quan thuế'"
            @hidden="resetForm" hide-footer size="lg">
            <div v-if="fetchingDetail" class="text-center py-5 text-primary">
                <b-spinner class="mb-2"></b-spinner>
                <div>Đang lấy thông tin chi tiết...</div>
            </div>

            <b-form @submit.prevent="saveItem" v-else>
                <div class="row">
                    <div class="col-md-6">
                        <b-form-group label="Mã CQT (*)" description="Mã định danh duy nhất (VD: 1001)">
                            <b-form-input v-model="form.code" required :disabled="isEditMode"
                                placeholder="Nhập mã..."></b-form-input>
                        </b-form-group>
                    </div>
                    <div class="col-md-6">
                        <b-form-group label="Cục thuế Tỉnh/Thành">
                            <b-form-input v-model="form.provinceName" placeholder="VD: Hà Nội"></b-form-input>
                        </b-form-group>
                    </div>
                </div>

                <b-form-group label="Tên Cơ quan thuế (*)">
                    <b-form-input v-model="form.name" required
                        placeholder="VD: Chi cục thuế Quận Cầu Giấy"></b-form-input>
                </b-form-group>

                <b-form-group label="Trực thuộc Cơ quan quản lý (Cấp cha)">
                    <b-form-select v-model="form.parentId" :options="filteredParentOptions">
                        <template #first>
                            <b-form-select-option :value="null" class="font-weight-bold">
                                -- Là cấp cao nhất (Không có cha) --
                            </b-form-select-option>
                        </template>
                    </b-form-select>
                </b-form-group>

                <b-form-group label="Trạng thái hoạt động">
                    <b-form-radio-group v-model="form.status"
                        :options="[{ text: 'Hiển thị', value: 1 }, { text: 'Ẩn', value: 0 }]"
                        name="radio-status"></b-form-radio-group>
                </b-form-group>

                <hr />

                <div class="text-right">
                    <b-button variant="secondary" @click="showModal = false" class="mr-2">
                        Đóng
                    </b-button>
                    <b-button type="submit" variant="primary" :disabled="processing">
                        <b-spinner small v-if="processing" class="mr-1"></b-spinner>
                        <b-icon icon="check-circle" v-else class="mr-1"></b-icon>
                        {{ isEditMode ? "Lưu thay đổi" : "Tạo mới" }}
                    </b-button>
                </div>
            </b-form>
        </b-modal>
    </div>
</template>

<script>
import axios from "axios";

export default {
    data() {
        return {
            // --- Config & Data Table ---
            loading: false,
            items: [],
            totalRows: 0,
            currentPage: 1,
            perPage: 10,
            filter: { keyword: "" },
            fields: [
                { key: "code", label: "Mã CQT", sortable: true },
                { key: "name", label: "Tên CQT", sortable: true },
                { key: "provinceName", label: "Tỉnh/Thành" },
                { key: "managerName", label: "CQT Quản lý" },
                { key: "status", label: "Trạng thái", class: "text-center" },
                { key: "created", label: "Ngày tạo", class: "text-right" },
                { key: "actions", label: "Thao tác", class: "text-center", thStyle: { width: "120px" } },
            ],

            // --- Config Modal & Form ---
            showModal: false,
            fetchingDetail: false, // Biến check loading khi mở modal edit
            processing: false,
            isEditMode: false,
            parentOptions: [],
            form: {
                id: null,
                code: "",
                name: "",
                provinceName: "",
                status: 1,
                parentId: null,
            },
        };
    },
    computed: {
        // Logic lọc: Khi đang sửa bản ghi A, xóa A khỏi danh sách cha để tránh chọn A làm cha của A
        filteredParentOptions() {
            if (!this.isEditMode || !this.form.id) {
                return this.parentOptions;
            }
            return this.parentOptions.filter((opt) => opt.value !== this.form.id);
        },
    },
    mounted() {
        this.fetchData();
        this.fetchParents();
    },
    methods: {
        getAuthHeader() {
            const token = localStorage.getItem("token-admin");
            return { Authorization: `Bearer ${token}` };
        },

        async fetchData() {
            this.loading = true;
            try {
                const config = {
                    params: {
                        page: this.currentPage - 1,
                        size: this.perPage,
                        keyword: this.filter.keyword,
                        sortDir: "desc",
                        sortField: "createdAt",
                    },
                    headers: this.getAuthHeader(),
                };
                // Lưu ý: Đã thêm prefix  vào đường dẫn API
                const response = await axios.get("/tax-authority", config);
                this.items = response.data.content;
                this.totalRows = response.data.totalElements;
            } catch (error) {
                this.handleError(error);
            } finally {
                this.loading = false;
            }
        },

        async fetchParents() {
            try {
                const config = { params: { size: 1000 }, headers: this.getAuthHeader() };
                const response = await axios.get("/tax-authority", config);

                this.parentOptions = response.data.content.map((item) => ({
                    value: item.id,
                    text: `${item.code} - ${item.name}`,
                }));
            } catch (e) {
                console.error("Lỗi load danh sách cha", e);
            }
        },

        // --- LOGIC MỚI: GỌI API CHI TIẾT KHI EDIT ---
        async openModal(item) {
            this.fetchParents(); // Load lại list cha mới nhất

            if (item) {
                // --- CHẾ ĐỘ EDIT ---
                this.isEditMode = true;
                this.showModal = true;
                this.fetchingDetail = true; // Bật loading trong modal

                try {
                    // Gọi API lấy chi tiết để đảm bảo dữ liệu mới nhất
                    const response = await axios.get(`/tax-authority/${item.id}`, {
                        headers: this.getAuthHeader()
                    });
                    const data = response.data;

                    // Map dữ liệu từ API vào Form
                    this.form = {
                        id: data.id,
                        code: data.code,
                        name: data.name,
                        provinceName: data.provinceName,
                        status: data.status,
                        // QUAN TRỌNG: Lấy ID của object parent để gán vào dropdown
                        parentId: data.parentId
                    };
                } catch (error) {
                    this.handleError(error);
                    this.showModal = false; // Đóng modal nếu lỗi
                } finally {
                    this.fetchingDetail = false; // Tắt loading
                }

            } else {
                // --- CHẾ ĐỘ CREATE ---
                this.isEditMode = false;
                this.resetForm();
                this.showModal = true;
            }
        },

        async saveItem() {
            this.processing = true;
            try {
                const headers = this.getAuthHeader();

                if (this.isEditMode) {
                    // Update
                    await axios.put(`/tax-authority/${this.form.id}`, this.form, { headers });
                    this.$bvToast.toast("Cập nhật thành công", {
                        variant: "success",
                        title: "Thông báo",
                        solid: true,
                    });
                } else {
                    // Create
                    await axios.post("/tax-authority", this.form, { headers });
                    this.$bvToast.toast("Thêm mới thành công", {
                        variant: "success",
                        title: "Thông báo",
                        solid: true,
                    });
                }

                this.showModal = false;
                this.fetchData();
            } catch (error) {
                this.handleError(error);
            } finally {
                this.processing = false;
            }
        },

        deleteItem(item) {
            this.$bvModal
                .msgBoxConfirm(`Bạn có chắc muốn xóa "${item.name}"?`, {
                    title: "Xác nhận xóa",
                    size: "sm",
                    buttonSize: "sm",
                    okVariant: "danger",
                    okTitle: "Xóa ngay",
                    cancelTitle: "Hủy",
                    centered: true,
                })
                .then(async (value) => {
                    if (value) {
                        try {
                            const headers = this.getAuthHeader();
                            await axios.delete(`/tax-authority/${item.id}`, { headers });

                            this.$bvToast.toast("Đã xóa bản ghi", { variant: "success", solid: true });
                            this.fetchData();
                        } catch (error) {
                            this.handleError(error);
                        }
                    }
                });
        },

        handleError(error) {
            console.error(error);
            if (error.response) {
                const s = error.response.status;
                if (s === 403) {
                    this.$bvToast.toast("Bạn không có quyền thực hiện thao tác này", {
                        variant: "warning",
                        solid: true,
                    });
                } else if (s === 401) {
                    this.$bvToast.toast("Phiên đăng nhập hết hạn", {
                        variant: "danger",
                        solid: true,
                    });
                    localStorage.removeItem("token-admin");
                    this.$router.push("/login");
                } else {
                    const msg =
                        error.response.data && error.response.data.message
                            ? error.response.data.message
                            : "Có lỗi xảy ra";
                    this.$bvToast.toast(msg, { variant: "danger", solid: true });
                }
            } else {
                this.$bvToast.toast("Lỗi kết nối máy chủ", { variant: "danger" });
            }
        },

        resetForm() {
            this.form = {
                id: null,
                code: "",
                name: "",
                provinceName: "",
                status: 1,
                parentId: null,
            };
            this.fetchingDetail = false;
        },

        handlePageChange(page) {
            this.currentPage = page;
            this.fetchData();
        },

        formatDate(dateString) {
            if (!dateString) return "";
            return new Date(dateString).toLocaleDateString("vi-VN", {
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
            });
        },
    },
    watch: {
        "filter.keyword"() {
            this.currentPage = 1;
            this.fetchData();
        },
    },
};
</script>

<style scoped>
.table th {
    vertical-align: middle;
    white-space: nowrap;
}
</style>