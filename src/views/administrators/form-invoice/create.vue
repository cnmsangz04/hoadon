<template>
    <div class="container-fluid py-3 form-invoice-form">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <h4 class="mb-0 font-weight-bold">{{ isEdit ? 'Cập nhật mẫu hệ thống' : 'Tạo mẫu hệ thống' }}</h4>
            <div>
                <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">Làm mới</b-button>
            </div>
        </div>

        <b-card class="shadow-sm">
            <b-form @submit.prevent="onSubmit">
                <b-row>
                    <b-col md="6">
                        <b-form-group label="Tên mẫu">
                            <b-form-input v-model.trim="form.name" required />
                        </b-form-group>
                    </b-col>

                    <b-col md="3">
                        <b-form-group label="Loại">
                            <b-form-select v-model.number="form.category" :options="categoryOptions" required />
                        </b-form-group>
                    </b-col>

                    <b-col md="3">
                        <b-form-group label="Thuế suất">
                            <b-form-select v-model.number="form.type" :options="typeOptions" required />
                        </b-form-group>
                    </b-col>
                </b-row>

                <b-row>
                    <b-col md="6">
                        <b-form-group label="Công ty">
                            <v-select v-model="form.companyId" :options="companyOptions" label="label"
                                :reduce="c => c.value" placeholder="Chọn công ty" />
                        </b-form-group>
                    </b-col>
                </b-row>

                <b-row>
                    <b-col md="4">
                        <b-form-group label="Ký hiệu (formCode + serial)">
                            <b-form-input v-model.trim="serialFull" placeholder="VD: CCTT" />
                        </b-form-group>
                    </b-col>
                    <b-col md="4">
                        <b-form-group label="Trạng thái">
                            <b-form-select v-model.number="form.status" :options="statusOptions" />
                        </b-form-group>
                    </b-col>
                    <b-col md="4">
                        <b-form-group label="Have code">
                            <b-form-select v-model.number="form.have_code" :options="haveCodeOptions" />
                        </b-form-group>
                    </b-col>
                </b-row>

                <b-row>
                    <b-col md="6">
                        <b-form-group label="Tập tin mẫu (XSLT)">
                            <input ref="fileInput" type="file" accept=".xslt,.xml,text/xml" @change="onFileChange"
                                class="d-none" />
                            <div class="d-flex gap-2 align-items-center">
                                <b-button size="sm" variant="success" @click="$refs.fileInput.click()">Chọn
                                    file</b-button>
                                <span class="text-muted">{{ fileName || 'Chưa chọn' }}</span>
                                <b-button v-if="fileName" size="sm" variant="link"
                                    @click.prevent="clearFile">Xóa</b-button>
                            </div>
                            <div class="mt-2 file-preview" v-if="filePreviewUrl || fileName">
                                <template v-if="filePreviewIsImage">
                                    <img :src="filePreviewUrl" alt="Preview"
                                        style="max-height:120px; max-width:240px; border-radius:4px; cursor:pointer;"
                                        @click.prevent="previewFile('file')" />
                                </template>
                                <template v-else>
                                    <div class="d-inline-flex align-items-center border rounded p-2">
                                        <span style="font-size:24px; margin-right:8px;">📄</span>
                                        <div>
                                            <div style="font-weight:600">{{ fileName }}</div>
                                            <div>
                                                <b-button size="sm" variant="link"
                                                    @click.prevent="previewFile('file')">Xem</b-button>
                                            </div>
                                        </div>
                                    </div>
                                </template>
                            </div>
                        </b-form-group>
                    </b-col>
                    <b-col md="6">
                        <b-form-group label="Ảnh mẫu">
                            <input ref="photoInput" type="file" accept=".png,.jpg,.jpeg" @change="onPhotoChange"
                                class="d-none" />
                            <div class="d-flex gap-2 align-items-center">
                                <b-button size="sm" variant="success" @click="$refs.photoInput.click()">Chọn
                                    ảnh</b-button>
                                <span class="text-muted">{{ photoName || 'Chưa chọn' }}</span>
                                <b-button v-if="photoName" size="sm" variant="link"
                                    @click.prevent="clearPhoto">Xóa</b-button>
                            </div>
                            <div class="mt-2" v-if="photoPreviewUrl">
                                <img :src="photoPreviewUrl" alt="Ảnh mẫu"
                                    style="max-height:120px; max-width:240px; border-radius:4px; cursor:pointer;"
                                    @click.prevent="previewFile('photo')" />
                            </div>
                        </b-form-group>
                    </b-col>
                </b-row>

                <div class="form-actions mt-3 d-flex justify-content-between">
                    <b-button variant="secondary" @click="$router.back()">Quay lại</b-button>
                    <b-button type="submit" :disabled="isSaving" variant="primary">{{ isEdit ? (isSaving ? 'Đang cập
                        nhật...' : 'Cập
                        nhật') : (isSaving ? 'Đang tạo...' : 'Tạo') }}</b-button>
                </div>
            </b-form>
        </b-card>

        <!-- Preview Modal -->
        <b-modal ref="filePreviewModal" hide-footer :title="filePreviewModalTitle || 'Xem file'" ok-only>
            <div v-if="filePreviewModalLoading" class="text-center py-3">Đang tải...</div>
            <div v-else>
                <div v-if="!filePreviewModalIsText">
                    <img :src="filePreviewModalImg" alt="Preview" style="max-width:100%; display:block; margin:auto;" />
                </div>
                <div v-else>
                    <pre
                        style="white-space:pre-wrap; max-height:60vh; overflow:auto; background:#f8f9fa; padding:12px; border-radius:6px;">
                {{ filePreviewModalContent }}</pre>
                </div>
            </div>
        </b-modal>

    </div>
</template>

<script>
import axios from '@/plugins/axios'
import vSelect from 'vue-select'
import 'vue-select/dist/vue-select.css'

export default {
    name: 'AdminFormInvoiceCreate',
    components: { vSelect },
    data() {
        return {
            isEdit: false,
            id: null,
            form: { name: '', companyId: null, category: null, type: null, status: 0, have_code: 0, file: null, photo: null },
            serialFull: '',
            fileInputObj: null,
            fileName: '',
            filePreviewUrl: '',
            filePreviewIsImage: false,
            filePreviewObjectUrl: false,
            photoInputObj: null,
            photoName: '',
            photoPreviewUrl: '',
            photoPreviewObjectUrl: false,
            // preview modal state
            filePreviewModalContent: '',
            filePreviewModalLoading: false,
            filePreviewModalTitle: '',
            filePreviewModalIsText: false,
            filePreviewModalImg: '',
            companyOptions: [],
            categoryOptions: [{ value: 1, text: 'Hóa đơn giá trị gia tăng' }, { value: 2, text: 'Hóa đơn bán hàng' }],
            typeOptions: [{ value: 1, text: 'Một thuế suất' }, { value: 2, text: 'Nhiều thuế suất' }],
            statusOptions: [{ value: 1, text: 'Kích hoạt' }, { value: 0, text: 'Chưa kích hoạt' }],
            haveCodeOptions: [{ value: 1, text: 'C' }, { value: 0, text: 'K' }],
            isSaving: false
        }
    },
    created() {
        const id = this.$route.params.id
        this.loadCompanies()
        if (id) this.load(id)
    },
    methods: {
        async loadCompanies() {
            try {
                const res = await axios.post('/administrator/company/list', {}, { params: { page: 0, size: 5000 } })
                const list = res.data?.content || []
                this.companyOptions = list.map(c => ({ value: Number(c.id), label: c.name || c.companyName || c.domain || `#${c.id}` }))
            } catch (e) {
                this.companyOptions = []
            }
        },
        reload() { if (this.isEdit && this.id) this.load(this.id) },
        onFileChange(e) {
            const f = e.target.files && e.target.files[0]
            if (!f) { this.fileInputObj = null; this.fileName = ''; this.filePreviewUrl = ''; this.filePreviewIsImage = false; this.filePreviewObjectUrl = false; return }
            this.fileInputObj = f; this.fileName = f.name
            // detect image type
            const lower = (f.name || '').toLowerCase()
            const isImage = f.type && f.type.startsWith('image/') || /\.(png|jpe?g|gif|webp)$/i.test(lower)
            if (isImage) {
                if (this.filePreviewObjectUrl && this.filePreviewUrl) URL.revokeObjectURL(this.filePreviewUrl)
                this.filePreviewUrl = URL.createObjectURL(f)
                this.filePreviewIsImage = true
                this.filePreviewObjectUrl = true
            } else {
                // non-image file (likely xslt/xml) - no image preview, but clear any object url
                if (this.filePreviewObjectUrl && this.filePreviewUrl) { URL.revokeObjectURL(this.filePreviewUrl) }
                this.filePreviewUrl = ''
                this.filePreviewIsImage = false
                this.filePreviewObjectUrl = false
            }
        },
        onPhotoChange(e) {
            const f = e.target.files && e.target.files[0]
            if (!f) { this.photoInputObj = null; this.photoName = ''; this.photoPreviewUrl = ''; this.photoPreviewObjectUrl = false; return }
            this.photoInputObj = f; this.photoName = f.name
            if (this.photoPreviewObjectUrl && this.photoPreviewUrl) URL.revokeObjectURL(this.photoPreviewUrl)
            this.photoPreviewUrl = URL.createObjectURL(f)
            this.photoPreviewObjectUrl = true
        },
        clearFile() {
            if (this.filePreviewObjectUrl && this.filePreviewUrl) { URL.revokeObjectURL(this.filePreviewUrl) }
            this.fileInputObj = null; this.fileName = ''; this.filePreviewUrl = ''; this.filePreviewIsImage = false; this.filePreviewObjectUrl = false; this.$refs.fileInput.value = null
        },
        clearPhoto() {
            if (this.photoPreviewObjectUrl && this.photoPreviewUrl) { URL.revokeObjectURL(this.photoPreviewUrl) }
            this.photoInputObj = null; this.photoName = ''; this.photoPreviewUrl = ''; this.photoPreviewObjectUrl = false; this.$refs.photoInput.value = null
        },
        async load(id) {
            try {
                const { data } = await axios.get(`/administrator/form-invoices/${id}`)
                this.isEdit = true; this.id = id
                this.form = { ...this.form, name: data.name, companyId: data.companyId || data.company_id || null, category: data.category, type: data.type, status: data.status, have_code: data.haveCode || data.have_code }
                this.serialFull = `${data.formCode || data.form_code || ''}${data.serial || ''}`
                // Use only basename for display titles (don't show full path)
                this.fileName = data.file ? (data.file.split('/').pop() || data.file) : ''
                this.photoName = data.photo ? (data.photo.split('/').pop() || data.photo) : ''
                // set preview urls from existing stored paths
                if (data.photo) {
                    this.photoPreviewUrl = data.photo
                    this.photoPreviewObjectUrl = false
                }
                if (data.file) {
                    // if file is image (unlikely), show preview; otherwise leave icon with URL so user can open
                    const lower = (data.file || '').toLowerCase()
                    if (/\.(png|jpe?g|gif|webp)$/i.test(lower)) {
                        this.filePreviewUrl = data.file
                        this.filePreviewIsImage = true
                        this.filePreviewObjectUrl = false
                    } else {
                        this.filePreviewUrl = data.file
                        this.filePreviewIsImage = false
                        this.filePreviewObjectUrl = false
                    }
                }
            } catch (e) {
                // ignore
            }
        },
        previewFile(source) {
            // source can be 'file' or 'photo' (if omitted, default to 'file')
            source = source || 'file';
            this.filePreviewModalContent = '';
            this.filePreviewModalLoading = true;
            this.filePreviewModalIsText = false;
            this.filePreviewModalImg = '';
            // prefer showing a friendly basename title instead of full path
            this.filePreviewModalTitle = (source === 'photo' ? this.photoName : this.fileName) || this.fileName || this.photoName || 'Xem file';

            if (source === 'file') {
                // If a local file was selected, prefer it (image -> show image, else read as text)
                if (this.fileInputObj) {
                    if (this.filePreviewIsImage && this.filePreviewUrl) {
                        this.filePreviewModalLoading = false;
                        this.filePreviewModalIsText = false;
                        this.filePreviewModalImg = this.filePreviewUrl;
                        this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                        return;
                    }
                    const reader = new FileReader();
                    reader.onload = (ev) => {
                        this.filePreviewModalContent = ev.target.result;
                        this.filePreviewModalIsText = true;
                        this.filePreviewModalLoading = false;
                        this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                    };
                    reader.onerror = () => {
                        this.filePreviewModalContent = 'Không thể đọc file';
                        this.filePreviewModalIsText = true;
                        this.filePreviewModalLoading = false;
                        this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                    };
                    reader.readAsText(this.fileInputObj);
                    return;
                }

                // If there is a file URL (remote saved file), handle it next (image -> show image, else fetch as text)
                if (this.filePreviewUrl) {
                    if (this.filePreviewIsImage) {
                        this.filePreviewModalLoading = false;
                        this.filePreviewModalIsText = false;
                        this.filePreviewModalImg = this.filePreviewUrl;
                        this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                        return;
                    }
                    axios.get(this.filePreviewUrl, { responseType: 'text' }).then(res => {
                        this.filePreviewModalContent = res.data;
                        this.filePreviewModalIsText = true;
                        this.filePreviewModalLoading = false;
                        this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                    }).catch(err => {
                        this.filePreviewModalContent = 'Không thể tải file để xem';
                        this.filePreviewModalIsText = true;
                        this.filePreviewModalLoading = false;
                        this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                    });
                    return;
                }

                // If no file available, fall back to photo preview
                if (this.photoPreviewUrl) {
                    this.filePreviewModalLoading = false;
                    this.filePreviewModalIsText = false;
                    this.filePreviewModalImg = this.photoPreviewUrl;
                    this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                    return;
                }
            } else if (source === 'photo') {
                // Photo requested: show photo if available
                if (this.photoPreviewUrl) {
                    this.filePreviewModalLoading = false;
                    this.filePreviewModalIsText = false;
                    this.filePreviewModalImg = this.photoPreviewUrl;
                    this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
                    return;
                }
                // Otherwise, fall back to file behavior
                if (this.fileInputObj || this.filePreviewUrl) {
                    this.previewFile('file');
                    return;
                }
            }

            // Final fallback
            this.filePreviewModalContent = 'Không có file để xem';
            this.filePreviewModalIsText = true;
            this.filePreviewModalLoading = false;
            this.$refs.filePreviewModal && this.$refs.filePreviewModal.show();
        },
        async onSubmit() {
            // Client-side validation with toast feedback
            if (!this.form.name || !this.form.name.trim()) {
                this.$bvToast && this.$bvToast.toast('Tên mẫu là bắt buộc', { title: 'Lỗi', variant: 'danger', solid: true })
                return
            }
            if (!this.serialFull || this.serialFull.trim().length < 2) {
                this.$bvToast && this.$bvToast.toast('Ký hiệu mẫu không hợp lệ (ví dụ: CCTT)', { title: 'Lỗi', variant: 'danger', solid: true })
                return
            }

            if (this.isSaving) return
            this.isSaving = true

            const fd = new FormData()
            if (this.id) fd.append('id', this.id)
            fd.append('name', this.form.name)
            if (this.form.category != null) fd.append('category', this.form.category)
            if (this.form.type != null) fd.append('type', this.form.type)
            fd.append('status', this.form.status)
            fd.append('have_code', this.form.have_code)
            fd.append('serial', this.serialFull)
            if (this.form.companyId != null) fd.append('companyId', this.form.companyId)
            if (this.fileInputObj) fd.append('file', this.fileInputObj)
            if (this.photoInputObj) fd.append('photo', this.photoInputObj)

            try {
                const { data } = await axios.post('/administrator/form-invoices/saveOrUpdate', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
                this.$bvToast && this.$bvToast.toast('Lưu thành công', { title: 'Thành công', variant: 'success', solid: true })
                this.$router.push({ name: 'admin-form-invoice-list' })
            } catch (e) {
                const msg = e?.response?.data?.message || 'Lỗi lưu mẫu'
                this.$bvToast && this.$bvToast.toast(msg, { title: 'Lỗi', variant: 'danger', solid: true })
            } finally {
                this.isSaving = false
            }
        }
    }
}
</script>

<style scoped>
.form-invoice-form .card.shadow-sm {
    border-radius: 10px;
}

.file-preview img {
    max-height: 120px;
    max-width: 240px;
    display: block;
}

.file-preview .d-inline-flex {
    min-height: 48px;
}
</style>
