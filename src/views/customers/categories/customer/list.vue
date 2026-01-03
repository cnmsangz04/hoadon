<template>
    <div class="container-fluid py-3 customer-list">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <h4 class="mb-0 font-weight-bold">Danh mục khách hàng</h4>
            <div>
                <b-button
                    size="sm"
                    variant="outline-primary"
                    class="mr-2"
                    @click="reload"
                >
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
                        <b-input-group-prepend is-text
                            ><i class="fas fa-search"></i
                        ></b-input-group-prepend>
                        <b-form-input
                            v-model.trim="filters.keyword"
                            placeholder="Tìm theo MST, Tên khách hàng hoặc Mã KH..."
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
                responsive
                small
                show-empty
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

                <template #cell(managedBy)="data">
                    <span class="text-muted small">{{
                        companyNameById[data.item.companyId] || "—"
                    }}</span>
                </template>

                <template #cell(companyName)="data">
                    <div class="font-weight-bold">
                        {{ data.item.companyName }}
                    </div>
                    <small class="text-muted" v-if="data.item.taxCode"
                        >MST: {{ data.item.taxCode }}</small
                    >
                </template>

                <template #cell(phone)="data">
                    <div class="font-weight-bold">{{ data.item.phone }}</div>
                </template>

                <template #cell(status)="data">
                    <b-badge
                        :variant="
                            data.item.status === 1 ? 'success' : 'secondary'
                        "
                    >
                        {{
                            data.item.status === 1
                                ? "Hoạt động"
                                : "Ngừng hoạt động"
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
                    </b-dropdown>
                </template>
            </b-table>

            <b-row class="mt-3" v-if="list.total > 0">
                <b-col cols="6" class="d-flex align-items-center">
                    <span class="text-muted small mr-2">Hiển thị</span>
                    <b-form-select
                        size="sm"
                        style="width: 80px"
                        v-model.number="list.per_page"
                        :options="pageSizes"
                        @change="onPageSizeChange"
                    />
                    <span class="text-muted small ml-2">
                        từ {{ list.from }} đến {{ list.to }} trong
                        {{ list.total }} bản ghi.
                    </span>
                </b-col>
                <b-col cols="6">
                    <b-pagination
                        align="right"
                        v-model="list.current_page"
                        :total-rows="list.total"
                        :per-page="list.per_page"
                        @change="onPageChange"
                        v-if="list.total > list.per_page"
                        size="sm"
                        pills
                    />
                </b-col>
            </b-row>
        </b-card>

        <b-modal
            ref="customerModal"
            :title="form.id ? 'Cập nhật khách hàng' : 'Thêm khách hàng mới'"
            ok-title="Lưu khách hàng"
            cancel-title="Bỏ qua"
            @ok.prevent="saveData"
            size="lg"
        >
            <b-form>
                <b-row>
                    <b-col md="12" class="mb-3">
                        <b-form-group
                            label="Công ty quản lý (Đơn vị chủ quản)"
                            label-class="font-weight-bold"
                        >
                            <b-form-select
                                v-model="form.companyId"
                                :options="companyOptions"
                            >
                                <template #first
                                    ><b-form-select-option :value="null"
                                        >-- Chọn đơn vị quản lý
                                        --</b-form-select-option
                                    ></template
                                >
                            </b-form-select>
                        </b-form-group>
                    </b-col>

                    <b-col md="6">
                        <b-form-group label="Mã khách hàng">
                            <b-form-input
                                v-model="form.code"
                                placeholder="KH001"
                                required
                            />
                        </b-form-group>
                    </b-col>
                    <b-col md="6">
                        <b-form-group label="Mã số thuế">
                            <b-form-input
                                v-model="form.taxCode"
                                placeholder="Nhập MST khách hàng"
                            />
                        </b-form-group>
                    </b-col>

                    <b-col md="6">
                        <b-form-group label="Tên khách hàng / Tên Công ty">
                            <b-form-input v-model="form.companyName" required />
                        </b-form-group>
                    </b-col>

                    <b-col md="6">
                        <b-form-group label="Số điện thoại">
                            <b-form-input v-model="form.phone" required />
                        </b-form-group>
                    </b-col>

                    <b-col md="6">
                        <b-form-group label="Người mua hàng">
                            <b-form-input v-model="form.buyerName" />
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
                        <b-form-group label="Địa chỉ">
                            <b-form-input v-model="form.address" />
                        </b-form-group>
                    </b-col>

                    <b-col md="6">
                        <b-form-group label="Số tài khoản">
                            <b-form-input v-model="form.bankAccountNumber" />
                        </b-form-group>
                    </b-col>
                    <b-col md="6">
                        <b-form-group label="Mở tại Ngân hàng">
                            <b-form-input v-model="form.bankName" />
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
                            form.status === 1 ? "Hoạt động" : "Ngừng hoạt động"
                        }}
                    </b-form-checkbox>
                </b-form-group>
            </b-form>
        </b-modal>
    </div>
</template>

<script>
import axios from "@/plugins/axios";

export default {
    data() {
        return {
            isBusy: false,
            isSaving: false,
            list: {
                data: [],
                total: 0,
                per_page: 10,
                current_page: 1,
                from: 0,
                to: 0,
            },
            filters: { keyword: "", status: null },
            pageSizes: [10, 20, 50, 100],
            companyOptions: [],
            companyNameById: {},
            statusOptions: [
                { value: 1, text: "Hoạt động" },
                { value: 0, text: "Tạm khóa" },
            ],
            fields: [
                { key: "index", label: "#" },
                { key: "code", label: "Mã KH" },
                { key: "companyName", label: "Khách hàng" },
                { key: "managedBy", label: "Công ty quản lý" },
                { key: "phone", label: "Điện thoại" },
                { key: "status", label: "Trạng thái", tdClass: "text-center" },
                { key: "option", label: "Thao tác", tdClass: "text-center" },
            ],
            form: {
                id: null,
                companyId: null,
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
            },
        };
    },
    mounted() {
        this.loadCompanies();
        this.loadData();
    },
    methods: {
        async loadCompanies() {
            try {
                const res = await axios.post(
                    "/administrator/company/list",
                    {},
                    { params: { page: 0, size: 5000 } }
                );
                const items = res.data?.data || res.data?.content || [];
                const map = {};
                this.companyOptions = items.map((c) => {
                    map[c.id] = c.name || c.companyName;
                    return { value: c.id, text: c.name || c.companyName };
                });
                this.companyNameById = map;
            } catch (e) {
                console.error("Lỗi load công ty", e);
            }
        },
        async loadData() {
            this.isBusy = true;
            try {
                const rawStatus = this.filters.status;
                const cleanStatus =
                    rawStatus === null || rawStatus === ""
                        ? null
                        : Number(rawStatus);

                const filterBody = {
                    keyword: this.filters.keyword || null,
                    status: cleanStatus,
                };

                const res = await axios.post(
                    "/categories/customer/list",
                    filterBody,
                    {
                        params: {
                            page: this.list.current_page - 1,
                            size: this.list.per_page,
                        },
                    }
                );
                const d = res.data;
                this.list.data = d.content || d.data || [];
                this.list.total = d.totalElements || d.total || 0;

                this.list.from = Number(
                    d.from ??
                        (this.list.total === 0
                            ? 0
                            : (this.list.current_page - 1) *
                                  this.list.per_page +
                              1)
                );
                const numberOfElements = Array.isArray(this.list.data)
                    ? this.list.data.length
                    : 0;
                this.list.to = Number(
                    d.to ??
                        (this.list.total === 0
                            ? 0
                            : this.list.from + numberOfElements - 1)
                );
            } catch (error) {
                console.error("Lỗi tải dữ liệu:", error);
                this.$bvToast.toast("Không thể tải danh sách khách hàng", {
                    variant: "danger",
                });
            } finally {
                this.isBusy = false;
            }
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

        applyFilters() {
            this.list.current_page = 1;
            this.loadData();
        },
        reload() {
            this.filters = { keyword: "", status: null };
            this.loadData();
        },
        openCreate() {
            this.form = {
                id: null,
                companyId: null,
                status: 1,
                code: "",
                companyName: "",
            };
            this.$refs.customerModal.show();
        },
        openEdit(item) {
            this.form = { ...item };
            this.$refs.customerModal.show();
        },
        async saveData() {
            if (!this.form.companyId)
                return this.$bvToast.toast("Vui lòng chọn công ty quản lý", {
                    variant: "warning",
                });
            try {
                await axios.post("/categories/customer/save", this.form);
                this.$bvToast.toast("Thành công", {
                    variant: "success",
                    title: "Thông báo",
                });
                this.$refs.customerModal.hide();
                this.loadData();
            } catch (e) {
                this.$bvToast.toast("Lỗi khi lưu", {
                    variant: "danger",
                    title: "Lỗi",
                });
            }
        },
        async toggleLock(item) {
            try {
                const newStatus = item.status === 1 ? 0 : 1;
                await axios.post("/categories/customer/save", {
                    ...item,
                    status: newStatus,
                });
                this.$bvToast.toast("Cập nhật trạng thái thành công", {
                    variant: "success",
                    title: "Thông báo",
                });
                this.loadData();
            } catch (e) {
                this.$bvToast.toast("Không thể thay đổi trạng thái", {
                    variant: "danger",
                    title: "Lỗi",
                });
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
</style>
