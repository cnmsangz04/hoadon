<template>
    <div class="container-fluid py-3 customer-list">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <h4 class="mb-0 font-weight-bold">
                Danh mục khách hàng
            </h4>
            <div>
                <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
                    <i class="fas fa-sync-alt"></i> Làm mới
                </b-button>
                <b-button size="sm" variant="success" @click="openCreate">
                    <i class="fas fa-plus"></i> Thêm khách hàng
                </b-button>
            </div>
        </div>

        <b-card class="mb-3 shadow-sm border-0">
            <b-row>
                <b-col md="5" class="mb-2">
                    <b-input-group>
                        <b-input-group-prepend is-text><i class="fas fa-search"></i></b-input-group-prepend>
                        <b-form-input
                            v-model.trim="filters.keyword"
                            placeholder="Tìm theo MST, Tên khách hàng hoặc Mã KH..."
                            @keyup.enter="applyFilters"
                        />
                    </b-input-group>
                </b-col>
                <b-col md="4" class="mb-2">
                    <b-form-select v-model="filters.status" :options="statusOptions">
                        <template #first>
                            <b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option>
                        </template>
                    </b-form-select>
                </b-col>
                <b-col md="3" class="text-right">
                    <b-button variant="primary" block @click="applyFilters">Tìm kiếm</b-button>
                </b-col>
            </b-row>
        </b-card>

        <b-card class="shadow-sm border-0">
            <b-table
                bordered hover responsive small show-empty
                :items="list.data"
                :fields="fields"
                :busy="isBusy"
                empty-text="Không tìm thấy khách hàng nào"
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

                <template #cell(companyName)="data">
                    <div class="font-weight-bold text-dark">{{ data.item.companyName }}</div>
                    <small class="text-muted" v-if="data.item.taxCode">MST: {{ data.item.taxCode }}</small>
                </template>

                <template #cell(managedBy)>
                    <span class="font-weight-bold text-dark">{{ currentCompanyName || '---' }}</span>
                </template>

                <template #cell(status)="data">
                    <b-badge :variant="data.item.status === 1 ? 'success' : 'secondary'">
                        {{ data.item.status === 1 ? "Hoạt động" : "Ngừng hoạt động" }}
                    </b-badge>
                </template>

                <template #cell(option)="{ item }">
                    <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
                        <template #button-content><i class="fas fa-ellipsis-h text-muted"></i></template>
                        <b-dropdown-item @click="openEdit(item)"><i class="fas fa-edit mr-2"></i>Cập nhật</b-dropdown-item>
                        <b-dropdown-item @click="toggleLock(item)">
                            <i class="fas mr-2" :class="item.status == 1 ? 'fa-lock text-warning' : 'fa-unlock text-success'"></i>
                            {{ item.status == 1 ? "Khóa" : "Mở khóa" }}
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
            ref="customerModal"
            :title="form.id ? 'Cập nhật khách hàng' : 'Thêm khách hàng mới'"
            ok-title="Lưu dữ liệu"
            cancel-title="Bỏ qua"
            @ok.prevent="saveData"
            size="lg"
            :busy="isSaving"
        >
            <b-form>
                <b-row>
                    <b-col md="12" class="mb-2">
                        <b-form-group label="Công ty của bạn" label-class="font-weight-bold">
                            <b-form-input
                                :value="currentCompanyName || 'Đang tải thông tin...'"
                                readonly
                                class="font-weight-bold"
                            />
                        </b-form-group>
                    </b-col>

                    <b-col md="6">
                        <b-form-group label="Mã khách hàng">
                            <b-form-input v-model="form.code" placeholder="KH001" :disabled="!!form.id" />
                        </b-form-group>
                    </b-col>
                    <b-col md="6">
                        <b-form-group label="Mã số thuế">
                            <b-form-input v-model="form.taxCode" placeholder="Nhập MST khách hàng" />
                        </b-form-group>
                    </b-col>

                    <b-col md="12">
                        <b-form-group label="Tên khách hàng / Tên Công ty đối tác">
                            <b-form-input v-model="form.companyName" required placeholder="Nhập tên đầy đủ của khách hàng" />
                        </b-form-group>
                    </b-col>

                    <b-col md="6">
                        <b-form-group label="Người mua hàng">
                            <b-form-input v-model="form.buyerName" placeholder="Tên người đại diện" />
                        </b-form-group>
                    </b-col>
                    <b-col md="6">
                        <b-form-group label="Số điện thoại">
                            <b-form-input v-model="form.phone" />
                        </b-form-group>
                    </b-col>

                    <b-col md="12">
                        <b-form-group label="Địa chỉ">
                            <b-form-input v-model="form.address" />
                        </b-form-group>
                    </b-col>

                    <b-col md="6">
                        <b-form-group label="Email nhận hóa đơn">
                            <b-form-input v-model="form.email" type="email" />
                        </b-form-group>
                    </b-col>
                    <b-col md="6">
                        <b-form-group label="Fax">
                            <b-form-input v-model="form.fax" />
                        </b-form-group>
                    </b-col>

                    <b-col md="6">
                        <b-form-group label="Số tài khoản ngân hàng">
                            <b-form-input v-model="form.bankAccountNumber" />
                        </b-form-group>
                    </b-col>
                    <b-col md="6">
                        <b-form-group label="Tại ngân hàng">
                            <b-form-input v-model="form.bankName" />
                        </b-form-group>
                    </b-col>
                </b-row>

                <b-form-group label="Ghi chú">
                    <b-form-textarea v-model="form.description" rows="2"></b-form-textarea>
                </b-form-group>

                <b-form-group label="Trạng thái">
                    <b-form-checkbox v-model="form.status" :value="1" :unchecked-value="0" switch>
                        {{ form.status === 1 ? "Đang hoạt động" : "Ngừng hoạt động" }}
                    </b-form-checkbox>
                </b-form-group>
            </b-form>
        </b-modal>
    </div>
</template>

<script>
import axios from "@/plugins/axios";
import PaginationBar from "@/views/components/pagination_bar.vue";

export default {
    components: { PaginationBar },
    data() {
        return {
            isBusy: false,
            isSaving: false,
            currentCompanyName: "",
            list: {
                data: [],
                total: 0,
                per_page: 10,
                current_page: 1,
                from: 0,
                to: 0,
            },
            pageSizes: [10, 20, 50, 100],
            filters: { keyword: "", status: null },
            statusOptions: [
                { value: 1, text: "Hoạt động" },
                { value: 0, text: "Ngừng hoạt động" },
            ],
            fields: [
                { key: "index", label: "#", tdClass: "text-center" },
                { key: "code", label: "Mã KH" },
                { key: "companyName", label: "Khách hàng" },
                { key: "managedBy", label: "Đơn vị quản lý" },
                { key: "phone", label: "Điện thoại" },
                { key: "status", label: "Trạng thái", tdClass: "text-center" },
                { key: "option", label: "Thao tác", tdClass: "text-center" },
            ],
            form: this.initForm(),
        };
    },
    mounted() {
        this.loadProfile();
        this.loadData();
    },
    methods: {
        initForm() {
            return {
                id: null,
                code: "",
                taxCode: "",
                companyName: "",
                buyerName: "",
                fax: "",
                address: "",
                phone: "",
                email: "",
                bankAccountNumber: "",
                bankName: "",
                description: "",
                status: 1,
            };
        },
        async loadProfile() {
            try {
                const res = await axios.post("/setting/profile/get");
                if (res.data) {
                    this.currentCompanyName = res.data.companyName || res.data.name;
                }
            } catch (e) {
                console.error("Lỗi lấy Profile:", e);
            }
        },
        async loadData() {
            this.isBusy = true;
            try {
                const res = await axios.post("/categories/customer/list", this.filters, {
                    params: {
                        page: this.list.current_page - 1,
                        size: this.list.per_page,
                    },
                });
                const d = res.data;
                this.list.data = d.content || d.data || [];
                this.list.total = d.totalElements || d.total || 0;
                this.list.from = d.from || 0;
                this.list.to = d.to || 0;
            } catch (error) {
                this.$bvToast.toast("Lỗi tải danh sách khách hàng", { variant: "danger" });
            } finally {
                this.isBusy = false;
            }
        },
        applyFilters() {
            this.list.current_page = 1;
            this.loadData();
        },
        reload() {
            this.filters = { keyword: "", status: null };
            this.applyFilters();
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
        openCreate() {
            this.form = this.initForm();
            this.$refs.customerModal.show();
        },
        openEdit(item) {
            this.form = { ...item };
            this.$refs.customerModal.show();
        },
        async saveData() {
            if (!this.form.companyName || !this.form.code) {
                return this.$bvToast.toast("Mã và Tên khách hàng là bắt buộc", { variant: "warning" });
            }
			
			if(this.form.email){
				const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
				if(!emailRegex.test(this.form.email)){
					return this.$bvToast.toast("Email không hợp lệ", {
						variant: "danger",
						title: "Lỗi nhập liệu"
					})
				}

			}
			
            this.isSaving = true;
            try {
                await axios.post("/categories/customer/save", this.form);
                this.$bvToast.toast("Lưu khách hàng thành công", { variant: "success" });
                this.$refs.customerModal.hide();
                this.loadData();
            } catch (e) {
                this.$bvToast.toast(e.response?.data?.message || "Lỗi lưu dữ liệu", { variant: "danger" });
            } finally {
                this.isSaving = false;
            }
        },
        async toggleLock(item) {
            try {
                const newStatus = item.status === 1 ? 0 : 1;
                await axios.post("/categories/customer/save", { ...item, status: newStatus });
                this.$bvToast.toast("Cập nhật trạng thái thành công", { variant: "success" });
                this.loadData();
            } catch (e) {
                this.$bvToast.toast("Thao tác thất bại", { variant: "danger" });
            }
        }
    }
};
</script>

<style scoped>
.customer-list .card { border-radius: 8px; }
.bg-light { background-color: #f1f3f5 !important; }
</style>
