<template>
    <div class="container-fluid py-3 tax-authorities">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <h4 class="mb-0 font-weight-bold">Danh sách Cơ quan thuế</h4>
            <div>
                <b-button size="sm" variant="outline-primary" class="mr-2" @click="onRefresh">
                    <i class="fas fa-sync-alt"></i> Làm mới
                </b-button>
                <b-button size="sm" variant="success" @click="openModal(null)">
                    <i class="fas fa-plus"></i> Thêm mới
                </b-button>
            </div>
        </div>

        <b-card class="mb-3 shadow-sm">
            <b-row>
                <b-col md="4" class="mb-2">
                    <b-input-group>
                        <b-input-group-prepend is-text>
                            <i class="fas fa-search text-muted"></i>
                        </b-input-group-prepend>
                        <b-form-input v-model.trim="filter.keyword" placeholder="Tìm theo mã / tên CQT..."
                            @keyup.enter="onFilter" />
                    </b-input-group>
                </b-col>

                <b-col md="4" class="mb-2">
                    <b-form-select v-model="filter.parentId" :options="parentOptionsForFilter">
                        <template #first>
                            <b-form-select-option :value="null">-- Tất cả cơ quan quản lý --</b-form-select-option>
                        </template>
                    </b-form-select>
                </b-col>

                <b-col md="2" class="mb-2">
                    <b-form-select v-model="filter.status" :options="statusOptions">
                        <template #first>
                            <b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option>
                        </template>
                    </b-form-select>
                </b-col>

                <b-col md="2" class="text-right">
                    <b-button size="sm" variant="primary" @click="onFilter" block>
                        <i class="fas fa-search mr-1"></i> Tìm kiếm
                    </b-button>
                </b-col>
            </b-row>
        </b-card>

        <b-card class="shadow-sm">
            <b-table bordered hover responsive small show-empty :items="list.data" :fields="fields" :busy="isBusy"
                empty-text="Không tìm thấy dữ liệu phù hợp" head-variant="light">
                <template #cell(index)="{ index }">
                    {{ index + 1 + (list.current_page - 1) * list.per_page }}
                </template>

                <template #cell(name)="data">
                    <span class="font-weight-bold text-primary">{{ data.item.name }}</span>
                    <div class="small text-muted" v-if="data.item.code">Mã: {{ data.item.code }}</div>
                </template>

                <template #cell(managerName)="data">
                    <span v-if="data.item.managerName" class="text-info">
                        <i class="fas fa-level-up-alt mr-1"></i> {{ data.item.managerName }}
                    </span>
                    <span v-else class="text-muted font-italic small">-- Cấp cao nhất --</span>
                </template>

                <template #cell(status)="data">
                    <b-badge :variant="data.item.status === 1 ? 'success' : 'secondary'">
                        {{ data.item.status === 1 ? 'Hoạt động' : 'Đã ẩn' }}
                    </b-badge>
                </template>

                <template #cell(createdAt)="data">
                    {{ formatDate(data.item.createdAt) }}
                </template>

                <template #cell(option)="data">
                    <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
                        <template #button-content>
                            <i class="fas fa-ellipsis-h text-muted"></i>
                        </template>
                        <b-dropdown-item href="#" @click.prevent="openModal(data.item)">
                            <i class="fas fa-edit mr-2 text-primary"></i> Cập nhật
                        </b-dropdown-item>
                        <b-dropdown-item href="#" @click.prevent="deleteItem(data.item)">
                            <i class="fas fa-trash-alt mr-2 text-danger"></i> Xóa bỏ
                        </b-dropdown-item>
                    </b-dropdown>
                </template>

                <template #table-busy>
                    <div class="mt-2">
                        <b-skeleton width="100%" height="20px" animated class="mb-2" />
                        <b-skeleton width="96%" height="20px" animated class="mb-2" />
                        <b-skeleton width="92%" height="20px" animated class="mb-2" />
                    </div>
                </template>
            </b-table>

            <div class="pagination-bar" v-if="list.total > 0 || isBusy">
                <b-row class="align-items-center">
                    <b-col md="6" cols="12">
                        <b-form inline class="justify-content-md-start justify-content-center">
                            <span class="mr-2 text-muted small">Hiển thị</span>
                            <b-form-select size="sm" class="custom-select-sm mr-2" v-model.number="list.per_page"
                                :options="pageSizes" @change="onPageSizeChange" style="width: 70px" />
                            <span class="text-muted small">
                                từ <b>{{ list.from }}</b> đến <b>{{ list.to }}</b> trong tổng <b>{{ list.total }}</b>
                            </span>
                        </b-form>
                    </b-col>
                    <b-col md="6" cols="12">
                        <b-pagination v-model="list.current_page" :total-rows="list.total" :per-page="list.per_page"
                            align="right" size="sm" class="my-0" @change="onPageChange"></b-pagination>
                    </b-col>
                </b-row>
            </div>
        </b-card>

        <b-modal ref="modalForm" :title="form.id ? 'Cập nhật Cơ quan thuế' : 'Thêm mới Cơ quan thuế'" hide-footer
            size="lg">
            <div v-if="fetchingDetail" class="text-center py-4">
                <b-spinner variant="primary"></b-spinner>
            </div>
            <b-form @submit.prevent="saveItem" v-else>
                <b-row>
                    <b-col md="6">
                        <b-form-group label="Mã CQT (*)">
                            <b-form-input v-model="form.code" required placeholder="Nhập mã số..." />
                        </b-form-group>
                    </b-col>
                    <b-col md="6">
                        <b-form-group label="Tỉnh/Thành phố">
                            <b-form-input v-model="form.provinceName" placeholder="VD: Hà Nội" />
                        </b-form-group>
                    </b-col>
                </b-row>

                <b-form-group label="Tên Cơ quan thuế (*)">
                    <b-form-input v-model="form.name" required placeholder="VD: Chi cục thuế Quận Cầu Giấy" />
                </b-form-group>

                <b-form-group label="Cơ quan quản lý (Cấp cha)">
                    <b-form-select v-model="form.parentId" :options="filteredParentOptionsForModal">
                        <template #first>
                            <b-form-select-option :value="null">-- Là cấp cao nhất --</b-form-select-option>
                        </template>
                    </b-form-select>
                </b-form-group>

                <b-form-group label="Trạng thái">
                    <b-form-radio-group v-model="form.status"
                        :options="[{ text: 'Hoạt động', value: 1 }, { text: 'Ẩn', value: 0 }]" />
                </b-form-group>

                <div class="text-right mt-4">
                    <b-button variant="secondary" class="mr-2" @click="$refs.modalForm.hide()">Hủy bỏ</b-button>
                    <b-button type="submit" variant="primary" :disabled="processing">
                        <b-spinner small v-if="processing" class="mr-1"></b-spinner>
                        Lưu thông tin
                    </b-button>
                </div>
            </b-form>
        </b-modal>
    </div>
</template>

<script>
import axios from "axios";

export default {
    name: "TaxAuthoritiesList",
    data() {
        return {
            isBusy: false,
            // Cấu trúc list đồng bộ với mẫu
            list: {
                current_page: 1,
                data: [],
                per_page: 10,
                total: 0,
                from: 0,
                to: 0
            },
            pageSizes: [10, 20, 50, 100],

            // Filter state
            filter: {
                keyword: "",
                parentId: null,
                status: null
            },
            statusOptions: [
                { value: 1, text: 'Hoạt động' },
                { value: 0, text: 'Đã ẩn' }
            ],
            parentOptions: [], // Dữ liệu thô danh sách cha

            // Table Config
            fields: [
                { key: "index", label: "#", thStyle: { width: "50px" }, class: "text-center" },
                { key: "name", label: "Thông tin CQT" },
                { key: "provinceName", label: "Tỉnh/Thành" },
                { key: "managerName", label: "Quản lý bởi" },
                { key: "createdAt", label: "Ngày tạo", class: "text-right" },
                { key: "status", label: "Trạng thái", class: "text-center", thStyle: { width: "100px" } },
                { key: "option", label: "Chức năng", class: "text-center", thStyle: { width: "80px" } }
            ],

            // Trạng thái form
            fetchingDetail: false,
            processing: false,
            form: {
                id: null,
                code: "",
                name: "",
                provinceName: "",
                parentId: null,
                status: 1
            }
        };
    },
    computed: {
        // Dropdown cho Bộ lọc bên ngoài (Hiện tất cả)
        parentOptionsForFilter() {
            return this.parentOptions.map(item => ({
                value: item.id,
                text: `${item.code ? item.code + ' - ' : ''}${item.name}`
            }));
        },
        // Dropdown cho Modal (Ẩn chính nó để tránh chọn mình làm cha)
        filteredParentOptionsForModal() {
            const opts = this.parentOptionsForFilter;
            if (!this.form.id) return opts;
            return opts.filter(opt => opt.value !== this.form.id);
        }
    },
    mounted() {
        this.loadParents(); // Tải danh sách cha trước để điền combobox
        this.loadData();
    },
    methods: {
        // --- LOAD DATA ---
        async loadData() {
            this.isBusy = true;
            try {
                const params = {
                    // Lưu ý: API Spring Boot thường tính page bắt đầu từ 0
                    // Frontend hiển thị page 1 -> Gửi lên page 0
                    page: this.list.current_page - 1,
                    size: this.list.per_page,
                    keyword: this.filter.keyword,
                    parentId: this.filter.parentId,
                    status: this.filter.status,
                    sortDir: "desc",
                    sortField: "id"
                };

                const res = await axios.get("/tax-authorities", { params });

                // Lấy data gốc từ response
                const responseData = res.data;

                // 1. Gán danh sách bản ghi
                this.list.data = responseData.content || [];

                // 2. Gán thông tin phân trang từ object "page" trong JSON
                if (responseData.page) {
                    this.list.total = responseData.page.totalElements || 0;
                    // API trả về page number bắt đầu từ 0, ta cộng 1 để khớp với UI
                    this.list.current_page = (responseData.page.number || 0) + 1;
                    this.list.per_page = responseData.page.size || this.list.per_page;
                } else {
                    // Dự phòng nếu API không trả về object page (trường hợp lỗi hoặc rỗng)
                    this.list.total = 0;
                }

                // 3. Tính toán From - To để hiển thị
                const numberOfElements = this.list.data.length;

                if (this.list.total === 0) {
                    this.list.from = 0;
                    this.list.to = 0;
                } else {
                    // Công thức: (Trang hiện tại - 1) * Số lượng mỗi trang + 1
                    this.list.from = (this.list.current_page - 1) * this.list.per_page + 1;
                    this.list.to = this.list.from + numberOfElements - 1;
                }

            } catch (e) {
                console.error(e);
                this.$toastr.error("Không thể tải dữ liệu");
            } finally {
                this.isBusy = false;
            }
        },

        async loadParents() {
            try {
                // Lấy danh sách để fill combobox (size lớn)
                const res = await axios.get("/tax-authorities", { params: { size: 1000, status: 1 } });
                this.parentOptions = res.data.content || [];
            } catch (e) {
                console.error("Lỗi load parents", e);
            }
        },

        // --- EVENTS ---
        onFilter() {
            this.list.current_page = 1;
            this.loadData();
        },

        onRefresh() {
            this.filter.keyword = "";
            this.filter.parentId = null;
            this.filter.status = null;
            this.onFilter();
            this.loadParents();
        },

        onPageChange(page) {
            this.list.current_page = page;
            this.loadData();
        },

        onPageSizeChange(size) {
            this.list.per_page = size;
            this.list.current_page = 1;
            this.loadData();
        },

        // --- CRUD ACTIONS ---
        async openModal(item) {
            this.loadParents(); // Refresh lại dropdown cha

            if (item) {
                // Edit Mode
                this.form.id = item.id;
                this.$refs.modalForm.show();
                this.fetchingDetail = true;

                try {
                    const res = await axios.get(`/tax-authorities/${item.id}`);
                    const data = res.data;
                    this.form = {
                        id: data.id,
                        code: data.code,
                        name: data.name,
                        provinceName: data.provinceName,
                        parentId: data.parentId,
                        status: data.status
                    };
                } catch (e) {
                    console.error(e);
                    this.$refs.modalForm.hide();
                } finally {
                    this.fetchingDetail = false;
                }
            } else {
                // Create Mode
                this.form = {
                    id: null,
                    code: "",
                    name: "",
                    provinceName: "",
                    parentId: null,
                    status: 1
                };
                this.$refs.modalForm.show();
            }
        },

        async saveItem() {
            this.processing = true;
            try {
                const payload = { ...this.form };
                if (this.form.id) {
                    await axios.put(`/tax-authorities/${this.form.id}`, payload);
                    this.$toastr.success("Cập nhật thành công");
                } else {
                    await axios.post("/tax-authorities", payload);
                    this.$toastr.success("Thêm mới thành công");
                }
                this.$refs.modalForm.hide();
                this.loadData();
                this.loadParents(); // Refresh lại list cha sau khi thêm/sửa
            } catch (e) {
                // Lỗi interceptor đã xử lý
            } finally {
                this.processing = false;
            }
        },

        deleteItem(item) {
            this.$bvModal.msgBoxConfirm(`Bạn có chắc muốn xóa "${item.name}"?`, {
                title: "Xác nhận xóa",
                size: "sm",
                buttonSize: "sm",
                okVariant: "danger",
                okTitle: "Xóa",
                cancelTitle: "Hủy",
                centered: true
            }).then(async (val) => {
                if (val) {
                    try {
                        await axios.delete(`/tax-authorities/${item.id}`);
                        this.$toastr.success("Đã xóa thành công");
                        this.loadData();
                        this.loadParents();
                    } catch (e) {
                        // error handled
                    }
                }
            });
        },

        formatDate(dateString) {
            if (!dateString) return "";
            return new Date(dateString).toLocaleDateString("vi-VN");
        }
    }
};
</script>

<style scoped>
/* Main Styles */
.tax-authorities .card.shadow-sm {
    border-radius: 10px;
    border: none;
}

.tax-authorities .table-hover tbody tr:hover {
    background-color: #fafbfd;
}

.tax-authorities .btn-outline-primary {
    border-color: #dfe7ff;
    color: #5969ff;
}

.tax-authorities .btn-outline-primary:hover {
    background: #eef3ff;
    color: #333;
}

.tax-authorities .table thead th {
    background: #f7f9fc;
    border-bottom: 1px solid #ecf0f6;
    border-top: none;
    color: #4a5568;
    font-weight: 700;
    vertical-align: middle;
}

/* Pagination Bar Styles (Copied & Adapted) */
.pagination-bar {
    padding-top: 10px;
    margin-top: 12px;
    border-top: 1px dashed #e5e7eb;
}

.pagination-bar .custom-select-sm {
    height: 30px;
    padding: 4px 10px;
    font-size: 13px;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
}

/* Pagination Buttons */
.pagination-bar ::v-deep .page-item .page-link {
    min-width: 32px;
    height: 30px;
    padding: 0 10px;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
    color: #374151;
    font-size: 13px;
    line-height: 28px;
    text-align: center;
    background-color: #fff;
    transition: all 0.15s ease;
    margin-left: 4px;
}

.pagination-bar ::v-deep .page-item:not(.active):not(.disabled) .page-link:hover {
    background-color: #f3f4f6;
    border-color: #d1d5db;
    color: #111827;
}

.pagination-bar ::v-deep .page-item.active .page-link {
    background-color: #2563eb;
    border-color: #2563eb;
    color: #fff;
    font-weight: 600;
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15);
}

.pagination-bar ::v-deep .page-item.disabled .page-link {
    background-color: #f9fafb;
    color: #9ca3af;
    border-color: #e5e7eb;
}

@media (max-width: 576px) {
    .pagination-bar {
        text-align: center;
    }
}
</style>