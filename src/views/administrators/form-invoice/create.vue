<template>
    <div class="container-fluid form-invoice-create-classic">

        <!-- Th? c? di?n c� header -->
        <div class="card">
            <div class="card-header bg-light">
                <h4 class="card-title mb-0">
                    <i class="fa fa-file-text-o"></i> Thông tin mẫu hóa đơn
                </h4>
            </div>
            <div class="card-body">
                <b-form @submit.prevent="onSubmit">
                    <!-- Tên mẫu -->
                    <div class="form-group row">
                        <label class="col-md-3 col-form-label">
                            Tên mẫu <span class="text-danger">*</span>
                        </label>
                        <div class="col-md-9">
                            <b-form-input 
                                v-model="form.name" 
                                required 
                                placeholder="Nhập tên mẫu hóa đơn"
                            />
                        </div>
                    </div>

                    <!-- Loại hóa đơn & Thuế suất -->
                    <div class="form-group row">
                        <label class="col-md-3 col-form-label">
                            Loại hóa đơn <span class="text-danger">*</span>
                        </label>
                        <div class="col-md-3">
                            <b-form-select 
                                v-model.number="form.category" 
                                :options="categoryOptions" 
                                required 
                            />
                        </div>
                        <label class="col-md-2 col-form-label">
                            Thuế suất <span class="text-danger">*</span>
                        </label>
                        <div class="col-md-4">
                            <b-form-select 
                                v-model.number="form.type" 
                                :options="typeOptions" 
                                required 
                            />
                        </div>
                    </div>

                    <!-- Hình thức & Trạng thái -->
                    <div class="form-group row">
                        <label class="col-md-3 col-form-label">Hình thức</label>
                        <div class="col-md-3">
                            <b-form-select 
                                v-model.number="form.have_code" 
                                :options="haveCodeOptions" 
                            />
                        </div>
                        <label class="col-md-2 col-form-label">Trạng thái</label>
                        <div class="col-md-4">
                            <b-form-select 
                                v-model.number="form.status" 
                                :options="statusOptions" 
                            />
                        </div>
                    </div>

                    <hr class="my-4" />

                    <!-- Tập tin mẫu -->
                    <div class="form-group row">
                        <label class="col-md-3 col-form-label">Tập tin mẫu (XSLT)</label>
                        <div class="col-md-9">
                            <input 
                                ref="fileInput" 
                                type="file" 
                                accept=".xslt,.xml,text/xml" 
                                @change="onFileChange" 
                                class="d-none" 
                            />
                            <div class="file-upload-classic">
                                <b-button variant="outline-success" size="sm" @click="$refs.fileInput.click()">
                                    <i class="fa fa-file-code-o"></i> Chọn file
                                </b-button>
                                <span class="file-name-text ml-2">{{ fileName || 'Chưa chọn file' }}</span>
                                <b-button 
                                    v-if="fileName" 
                                    variant="link" 
                                    size="sm" 
                                    class="text-danger ml-2" 
                                    @click.prevent="clearFile"
                                >
                                    <i class="fa fa-times"></i> Xóa
                                </b-button>
                            </div>
                            <div class="file-preview-classic mt-2" v-if="filePreviewUrl || fileName">
                                <template v-if="filePreviewIsImage">
                                    <img 
                                        :src="filePreviewUrl" 
                                        alt="Preview" 
                                        class="preview-img" 
                                        @click.prevent="previewFile('file')" 
                                    />
                                </template>
                                <template v-else>
                                    <div class="file-info-classic">
                                        <i class="fa fa-file-text-o fa-2x text-muted"></i>
                                        <div class="ml-3">
                                            <div class="filename-display">{{ fileName }}</div>
                                            <a href="#" @click.prevent="previewFile('file')" class="text-primary small">
                                                <i class="fa fa-eye"></i> Xem nội dung
                                            </a>
                                        </div>
                                    </div>
                                </template>
                            </div>
                        </div>
                    </div>

                    <!-- Ảnh mẫu -->
                    <div class="form-group row">
                        <label class="col-md-3 col-form-label">Ảnh mẫu</label>
                        <div class="col-md-9">
                            <input 
                                ref="photoInput" 
                                type="file" 
                                accept=".png,.jpg,.jpeg" 
                                @change="onPhotoChange" 
                                class="d-none" 
                            />
                            <div class="file-upload-classic">
                                <b-button variant="outline-success" size="sm" @click="$refs.photoInput.click()">
                                    <i class="fa fa-image"></i> Chọn ảnh
                                </b-button>
                                <span class="file-name-text ml-2">{{ photoName || 'Chưa chọn ảnh' }}</span>
                                <b-button 
                                    v-if="photoName" 
                                    variant="link" 
                                    size="sm" 
                                    class="text-danger ml-2" 
                                    @click.prevent="clearPhoto"
                                >
                                    <i class="fa fa-times"></i> Xóa
                                </b-button>
                            </div>
                            <div class="file-preview-classic mt-2" v-if="photoPreviewUrl">
                                <img 
                                    :src="photoPreviewUrl" 
                                    alt="Ảnh mẫu" 
                                    class="preview-img" 
                                    @click.prevent="previewFile('photo')" 
                                />
                            </div>
                        </div>
                    </div>

                    <hr class="my-4" />

                    <!-- N�t h�nh d?ng -->
                    <div class="form-group row mb-0">
                        <div class="col-md-12 text-right">
                             <b-button variant="secondary" size="sm" @click="$router.back()">
			                    <i class="fa fa-arrow-left"></i> Quay lại
			                </b-button>
                            <b-button 
                                type="submit" 
                                :disabled="isSaving" 
                                variant="primary" 
                                class="ml-2"
                                size="sm"
                            >
                                <i class="fa fa-save"></i> 
                                {{ isEdit ? (isSaving ? 'Đang cập nhật...' : 'Cập nhật') : (isSaving ? 'Đang tạo...' : 'Lưu') }}
                            </b-button>
                        </div>
                    </div>
                </b-form>
            </div>
        </div>

        <!-- Modal xem tru?c -->
        <b-modal ref="filePreviewModal" hide-footer :title="filePreviewModalTitle || 'Xem file'" size="lg">
            <div v-if="filePreviewModalLoading" class="text-center py-4">
                <i class="fa fa-spinner fa-spin fa-2x"></i>
                <p class="mt-2">Đang tải...</p>
            </div>
            <div v-else>
                <div v-if="!filePreviewModalIsText" class="text-center">
                    <img :src="filePreviewModalImg" alt="Preview" style="max-width:100%;" />
                </div>
                <div v-else>
                    <pre class="preview-code">{{ filePreviewModalContent }}</pre>
                </div>
            </div>
        </b-modal>
    </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
    name: 'AdminFormInvoiceCreate',
    data() {
        return {
            isEdit: false,
            id: null,
            form: {
                name: '',
                category: 1, // mặc định: Hóa đơn GTGT
                type: 1,     // mặc định: Một thuế suất
                status: 0,
                have_code: 1, // mặc định: Có mã
                file: null,
                photo: null
            },
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
            categoryOptions: [
                { value: 1, text: 'Hóa đơn giá trị gia tăng' },
                { value: 2, text: 'Hóa đơn bán hàng' }
            ],
            typeOptions: [
                { value: 1, text: 'Một thuế suất' },
                { value: 2, text: 'Nhiều thuế suất' }
            ],
            statusOptions: [
                { value: 1, text: 'Kích hoạt' },
                { value: 0, text: 'Chưa kích hoạt' }
            ],
            haveCodeOptions: [
                { value: 1, text: 'Có mã' },
                { value: 0, text: 'Không mã' }
            ],
            isSaving: false
        }
    },
    created() {
        const id = this.$route.params.id
        if (id) this.load(id)
    },
    methods: {
        reload() { 
            if (this.isEdit && this.id) this.load(this.id) 
        },
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
                this.isEdit = true
                this.id = id
                this.form = {
                    ...this.form,
                    name: data.name,
                    category: data.category != null ? data.category : 1,
                    type: data.type != null ? data.type : 1,
                    status: data.status != null ? data.status : 0,
                    have_code: data.haveCode != null ? data.haveCode : (data.have_code != null ? data.have_code : 1)
                }
                this.fileName = data.file ? (data.file.split('/').pop() || data.file) : ''
                this.photoName = data.photo ? (data.photo.split('/').pop() || data.photo) : ''
                if (data.photo) {
                    this.photoPreviewUrl = data.photo
                    this.photoPreviewObjectUrl = false
                }
                if (data.file) {
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
                console.error('Load error:', e)
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
            if (!this.form.name || !this.form.name.trim()) {
                this.$bvToast && this.$bvToast.toast('Tên mẫu là bắt buộc', { 
                    title: 'Lỗi', 
                    variant: 'danger', 
                    solid: true 
                })
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
            // không gửi companyId, serial; backend tự thiết lập
            if (this.fileInputObj) fd.append('file', this.fileInputObj)
            if (this.photoInputObj) fd.append('photo', this.photoInputObj)

            try {
                await axios.post('/administrator/form-invoices/saveOrUpdate', fd, { 
                    headers: { 'Content-Type': 'multipart/form-data' } 
                })
                this.$bvToast && this.$bvToast.toast('Lưu thành công', { 
                    title: 'Thành công', 
                    variant: 'success', 
                    solid: true 
                })
                this.$router.push({ name: 'admin-form-invoice-list' })
            } catch (e) {
                const msg = e?.response?.data?.message || 'Lỗi lưu mẫu'
                this.$bvToast && this.$bvToast.toast(msg, { 
                    title: 'Lỗi', 
                    variant: 'danger', 
                    solid: true 
                })
            } finally {
                this.isSaving = false
            }
        }
    }
}
</script>

<style scoped>
/* ====== Classic Page Title Box ====== */
.form-invoice-create-classic {
    padding: 20px 15px;
}

.page-title-box {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 2px solid #e9ecef;
}

.page-title {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    color: #333;
}

.page-title-right {
    display: flex;
    gap: 8px;
}

/* ====== Classic Card ====== */
.card {
    border: 1px solid #dee2e6;
    border-radius: 4px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.card-header {
    background-color: #f8f9fa;
    border-bottom: 1px solid #dee2e6;
    padding: 12px 20px;
}

.card-title {
    font-size: 16px;
    font-weight: 600;
    color: #495057;
}

.card-body {
    padding: 25px 20px;
}

/* ====== Classic Form Layout ====== */
.form-group.row {
    margin-bottom: 20px;
}

.col-form-label {
    font-weight: 500;
    color: #495057;
    padding-top: 8px;
    text-align: right;
}

.form-control,
.custom-select {
    border: 1px solid #ced4da;
    border-radius: 3px;
    padding: 6px 12px;
    font-size: 14px;
}

.form-control:focus,
.custom-select:focus {
    border-color: #80bdff;
    box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
}

/* ====== File Upload Classic ====== */
.file-upload-classic {
    display: flex;
    align-items: center;
}

.file-name-text {
    color: #6c757d;
    font-size: 14px;
    max-width: 400px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

/* ====== File Preview Classic ====== */
.file-preview-classic {
    border: 1px solid #dee2e6;
    border-radius: 3px;
    padding: 10px;
    background-color: #f8f9fa;
}

.preview-img {
    max-width: 200px;
    max-height: 150px;
    border: 1px solid #dee2e6;
    border-radius: 3px;
    cursor: pointer;
    transition: transform 0.2s;
}

.preview-img:hover {
    transform: scale(1.05);
}

.file-info-classic {
    display: flex;
    align-items: center;
    padding: 8px;
}

.filename-display {
    font-weight: 500;
    color: #495057;
    margin-bottom: 4px;
}

/* ====== Preview Modal Code ====== */
.preview-code {
    white-space: pre-wrap;
    max-height: 500px;
    overflow: auto;
    background-color: #f4f4f4;
    border: 1px solid #ddd;
    padding: 15px;
    border-radius: 3px;
    font-family: 'Courier New', Courier, monospace;
    font-size: 13px;
    color: #333;
}

/* ====== Buttons ====== */
.btn {
    border-radius: 3px;
    font-weight: 500;
}

.btn-primary {
    background-color: #007bff;
    border-color: #007bff;
}

.btn-primary:hover {
    background-color: #0056b3;
    border-color: #004085;
}

.btn-secondary {
    background-color: #6c757d;
    border-color: #6c757d;
}

.btn-info {
    background-color: #17a2b8;
    border-color: #17a2b8;
}

.btn-outline-success {
    color: #28a745;
    border-color: #28a745;
}

.btn-outline-success:hover {
    background-color: #28a745;
    color: white;
}

/* ====== Utilities ====== */
.text-danger {
    color: #dc3545 !important;
}

hr {
    border-top: 1px solid #dee2e6;
}

/* ====== Responsive ====== */
@media (max-width: 768px) {
    .col-form-label {
        text-align: left;
        padding-top: 0;
        margin-bottom: 8px;
    }
    
    .page-title-box {
        flex-direction: column;
        align-items: flex-start;
    }
    
    .page-title-right {
        margin-top: 10px;
        width: 100%;
    }
}
</style>
    min-height: 48px;
}
</style>
