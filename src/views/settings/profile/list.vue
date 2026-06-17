<template>
  <div class="right_col" role="main">
    <div class="page-content">
      <b-container fluid>
        <div class="page-header mb-2">
          <h1 class="page-title">Hồ sơ</h1>
        </div>

        <!-- Thông tin đơn vị -->
        <b-card class="contact-info">
          <div class="title-contact">
            <h6 class="d-inline-block">Thông tin đơn vị</h6>
            <div class="edit-icon" v-show="!formInfo">
              <span @click="editInfo"><i class="far fa-edit"></i></span>
            </div>
          </div>

          <!-- Chế độ xem -->
          <div class="contact-info-view" v-if="!formInfo">
            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Mã số thuế:</b-col>
                  <b-col cols="8">{{ frmData.taxCode }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Ngành nghề:</b-col>
                  <b-col cols="8">{{ frmData.companyBusiness }}</b-col>
                </b-row>
              </b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="2" class="font-weight-bold">Tên đơn vị:</b-col>
              <b-col cols="10">{{ frmData.companyName }}</b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="2" class="font-weight-bold">Địa chỉ:</b-col>
              <b-col cols="10">{{ frmData.companyAddress }}</b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="3" v-if="frmData.companyLogo">
                <p class="font-weight-bold">Logo:</p>
                <img height="50" :src="frmData.companyLogo" />
              </b-col>
              <b-col cols="3" v-if="frmData.companyFavicon">
                <p class="font-weight-bold">Favicon:</p>
                <img height="50" :src="frmData.companyFavicon" />
              </b-col>
            </b-row>
          </div>

          <!-- Chế độ sửa -->
          <b-form novalidate @submit.prevent="onSubmitInfo" v-else>
            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Mã số thuế"
                  label-for="taxcode"
                  label-class="required"
                >
                  <b-form-input
                    id="taxcode"
                    type="text"
                    placeholder="Mã số thuế doanh nghiệp"
                    disabled
                    v-model="frmData.taxCode"
                  />
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Ngành nghề"
                  label-for="business"
                  label-class="required"
                  :state="state('companyBusiness')"
                >
                  <b-form-input
                    id="business"
                    type="text"
                    placeholder="Nhập ngành nghề"
                    required
                    v-model.trim="frmInfo.companyBusiness"
                    :state="state('companyBusiness')"
                  />
                  <b-form-invalid-feedback :state="state('companyBusiness')">
                    {{ invalidFeedback('companyBusiness') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12">
                <b-form-group
                  label="Tên đơn vị"
                  label-for="CompanyName"
                  label-class="required"
                  :state="state('companyName')"
                >
                  <b-form-input
                    id="CompanyName"
                    type="text"
                    placeholder="Nhập tên đơn vị"
                    required
                    v-model.trim="frmInfo.companyName"
                    :state="state('companyName')"
                  />
                  <b-form-invalid-feedback :state="state('companyName')">
                    {{ invalidFeedback('companyName') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12">
                <b-form-group
                  label="Địa chỉ"
                  label-for="CompanyAddress"
                  label-class="required"
                  :state="state('companyAddress')"
                >
                  <b-form-input
                    id="CompanyAddress"
                    type="text"
                    placeholder="Nhập địa chỉ đơn vị"
                    required
                    v-model.trim="frmInfo.companyAddress"
                    :state="state('companyAddress')"
                  />
                  <b-form-invalid-feedback :state="state('companyAddress')">
                    {{ invalidFeedback('companyAddress') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="3" md="3">
                <b-form-group
                  label="Logo"
                  label-for="logo"
                  class="profile-upload-field text-center"
                  :state="state('logo')"
                >
                  <input
                    type="file"
                    ref="logo"
                    class="d-none"
                    @change="onUploadLogo"
                    accept=".png, .jpg, .jpeg"
                  />
                  <b-button
                    variant="success"
                    @click="$refs.logo.click()"
                    class="mb-1"
                    size="sm"
                  >
                    {{ logoName || "Chọn logo" }}
                  </b-button>
                  <p class="font-italic text-success">
                    Kích thước hình tiêu chuẩn 150px x 50px
                  </p>
                  <b-form-invalid-feedback :state="state('logo')">
                    {{ invalidFeedback('logo') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>

              <b-col cols="3" md="3">
                <b-form-group
                  label="Favicon"
                  label-for="Favicon"
                  class="profile-upload-field text-center"
                  :state="state('favicon')"
                >
                  <input
                    type="file"
                    ref="favicon"
                    class="d-none"
                    @change="onUploadFavicon"
                    accept=".png, .jpg, .jpeg"
                  />
                  <b-button
                    variant="success"
                    @click="$refs.favicon.click()"
                    class="mb-1"
                    size="sm"
                  >
                    {{ faviconName || "Chọn favicon" }}
                  </b-button>
                  <p class="font-italic text-success">
                    Kích thước hình tiêu chuẩn 50px x 50px
                  </p>
                  <b-form-invalid-feedback :state="state('favicon')">
                    {{ invalidFeedback('favicon') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <div class="profile-form-actions">
              <b-button size="sm" variant="light" @click="closeForm('formInfo')">
                Hủy
              </b-button>
              <b-button type="submit" size="sm" variant="primary" v-if="!buttonInfo">
                Cập nhật
              </b-button>
              <b-button type="button" size="sm" variant="secondary" disabled v-else>
                <b-spinner small type="grow"></b-spinner>
                Đang lưu...
              </b-button>
            </div>
          </b-form>
        </b-card>

        <!-- Thông tin người đại diện pháp luật -->
        <b-card class="contact-info mt-3">
          <div class="title-contact">
            <h6 class="d-inline-block">Thông tin người đại diện pháp luật</h6>
            <div class="edit-icon" v-show="!formRepresent">
              <span @click="editRepresent"><i class="far fa-edit"></i></span>
            </div>
          </div>

          <!-- Chế độ xem -->
          <div class="contact-info-view" v-if="!formRepresent">
            <b-row class="pt-2 pb-2">
              <b-col cols="2" class="font-weight-bold">Họ và tên:</b-col>
              <b-col cols="4">{{ frmData.representName }}</b-col>
              <b-col cols="2" class="font-weight-bold">Điện thoại:</b-col>
              <b-col cols="4">{{ frmData.representPhone }}</b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="2" class="font-weight-bold">Căn cước công dân:</b-col>
              <b-col cols="4">{{ frmData.representCitizenIdent }}</b-col>
              <b-col cols="2" class="font-weight-bold">Số hộ chiếu:</b-col>
              <b-col cols="4">{{ frmData.representPassPort }}</b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="2" class="font-weight-bold">Ngày sinh:</b-col>
              <b-col cols="4">{{ formatter(frmData.representDateBirth) }}</b-col>
              <b-col cols="2" class="font-weight-bold">Giới tính:</b-col>
              <b-col cols="4">
                <span>
                  {{
                    frmData.representGender == 1
                      ? "Nam"
                      : frmData.representGender == 0
                      ? "Nữ"
                      : ""
                  }}
                </span>
              </b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="2" class="font-weight-bold">Email:</b-col>
              <b-col cols="4">{{ frmData.representMail }}</b-col>
            </b-row>
          </div>

          <!-- Chế độ sửa -->
          <b-form novalidate @submit.prevent="onsubmitRepresent" v-else>
            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Họ và Tên"
                  label-for="represent-person"
                  label-class="required"
                  :state="state('representName')"
                >
                  <b-form-input
                    id="represent-person"
                    type="text"
                    placeholder="Nhập họ và tên"
                    v-model.trim="frmRepresent.representName"
                    :state="state('representName')"
                  />
                  <b-form-invalid-feedback :state="state('representName')">
                    {{ invalidFeedback('representName') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Điện thoại"
                  label-for="represent-phone"
                  label-class="required"
                  :state="state('representPhone')"
                >
                  <b-form-input
                    id="represent-phone"
                    type="tel"
                    placeholder="Nhập số điện thoại"
                    required
                    v-model.trim="frmRepresent.representPhone"
                    :state="state('representPhone')"
                  />
                  <b-form-invalid-feedback :state="state('representPhone')">
                    {{ invalidFeedback('representPhone') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Căn cước công dân"
                  label-for="represent-citizen-ident"
                  :state="state('representCitizenIdent')"
                >
                  <b-form-input
                    id="represent-citizen-ident"
                    type="text"
                    placeholder="Nhập căn cước công dân"
                    v-model.trim="frmRepresent.representCitizenIdent"
                    :state="state('representCitizenIdent')"
                  />
                  <b-form-invalid-feedback :state="state('representCitizenIdent')">
                    {{ invalidFeedback('representCitizenIdent') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Số hộ chiếu"
                  label-for="represent-pass-port"
                  :state="state('representPassPort')"
                >
                  <b-form-input
                    id="represent-pass-port"
                    type="text"
                    placeholder="Nhập số hộ chiếu"
                    v-model.trim="frmRepresent.representPassPort"
                    :state="state('representPassPort')"
                  />
                  <b-form-invalid-feedback :state="state('representPassPort')">
                    {{ invalidFeedback('representPassPort') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Ngày sinh:"
                  label-for="represent-date-birth"
                  label-class="required"
                  breakpoint="md"
                  :state="state('manualDate')"
                >
                  <b-input-group class="mb-3">
                    <b-form-input
                      id="represent-date-birth"
                      v-model="manualDate"
                      type="text"
                      placeholder="dd/mm/yyyy"
                      @input="parseManualDate"
                      autocomplete="off"
                      :state="state('manualDate')"
                    />
                    <b-input-group-append>
                      <b-form-datepicker
                        v-model="frmRepresent.representDateBirth"
                        locale="vi"
                        size="sm"
                        button-only
                        right
                        :date-format-options="{
                          year: 'numeric',
                          month: '2-digit',
                          day: '2-digit'
                        }"
                        :min="new Date('1900-01-01')"
                        :max="new Date()"
                        :show-decade-nav="true"
                        @input="syncManualDate"
                      />
                    </b-input-group-append>
                  </b-input-group>
                  <b-form-invalid-feedback :state="state('manualDate')">
                    {{ invalidFeedback('manualDate') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Giới tính"
                  label-for="represent-gender"
                  label-class="required"
                  :state="state('representGender')"
                >
                  <b-form-radio-group
                    id="represent-gender"
                    v-model="frmRepresent.representGender"
                    :options="options.gender"
                    name="gender"
                    :state="state('representGender')"
                  />
                  <b-form-invalid-feedback :state="state('representGender')">
                    {{ invalidFeedback('representGender') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12">
                <b-form-group
                  label="Email"
                  label-for="represent-email"
                  label-class="required"
                  :state="state('representMail')"
                >
                  <b-form-input
                    id="represent-email"
                    type="email"
                    placeholder="Nhập địa chỉ email"
                    required
                    v-model.trim="frmRepresent.representMail"
                    :state="state('representMail')"
                  />
                  <b-form-invalid-feedback :state="state('representMail')">
                    {{ invalidFeedback('representMail') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <div class="profile-form-actions">
              <b-button size="sm" variant="light" @click="closeForm('formRepresent')">
                Hủy
              </b-button>
              <b-button type="submit" size="sm" variant="primary" v-if="!buttonRepresent">
                Cập nhật
              </b-button>
              <b-button type="button" size="sm" variant="secondary" disabled v-else>
                <b-spinner small type="grow"></b-spinner>
                Đang lưu...
              </b-button>
            </div>
          </b-form>
        </b-card>

        <!-- Thông tin hiển thị trên hóa đơn điện tử -->
        <b-card class="contact-info mt-3">
          <div class="title-contact">
            <h6 class="d-inline-block">Thông tin hiển thị trên hóa đơn điện tử</h6>
            <div class="edit-icon" v-show="!formInvoice">
              <span @click="editInfoInvoice"><i class="far fa-edit"></i></span>
            </div>
          </div>

          <!-- Chế độ xem -->
          <div class="contact-info-view" v-if="!formInvoice">
            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Email:</b-col>
                  <b-col cols="8">{{ frmData.invoiceEmail }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Điện thoại:</b-col>
                  <b-col cols="8">{{ frmData.invoicePhone }}</b-col>
                </b-row>
              </b-col>
            </b-row>
            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Fax:</b-col>
                  <b-col cols="8">{{ frmData.invoiceFax }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Website:</b-col>
                  <b-col cols="8">{{ frmData.invoiceWebsite }}</b-col>
                </b-row>
              </b-col>
            </b-row>
          </div>

          <!-- Chế độ sửa -->
          <b-form novalidate @submit.prevent="onSubmitInfoInvoice" v-else>
            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Email"
                  label-for="invoice-email"
                  label-class="required"
                  :state="state('invoiceEmail')"
                >
                  <b-form-input
                    id="invoice-email"
                    type="email"
                    placeholder="Nhập địa chỉ email"
                    v-model.trim="frmDataInvoice.invoiceEmail"
                    :state="state('invoiceEmail')"
                  />
                  <b-form-invalid-feedback :state="state('invoiceEmail')">
                    {{ invalidFeedback('invoiceEmail') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Điện thoại"
                  label-for="invoice-phone"
                  label-class="required"
                  :state="state('invoicePhone')"
                >
                  <b-form-input
                    id="invoice-phone"
                    type="tel"
                    placeholder="Nhập số điện thoại"
                    required
                    v-model.trim="frmDataInvoice.invoicePhone"
                    :state="state('invoicePhone')"
                  />
                  <b-form-invalid-feedback :state="state('invoicePhone')">
                    {{ invalidFeedback('invoicePhone') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Fax"
                  label-for="invoice-fax"
                  label-class="required"
                  :state="state('invoiceFax')"
                >
                  <b-form-input
                    id="invoice-fax"
                    type="tel"
                    placeholder="Nhập số fax"
                    required
                    v-model.trim="frmDataInvoice.invoiceFax"
                    :state="state('invoiceFax')"
                  />
                  <b-form-invalid-feedback :state="state('invoiceFax')">
                    {{ invalidFeedback('invoiceFax') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Website"
                  label-for="invoice-website"
                  label-class="required"
                  :state="state('invoiceWebsite')"
                >
                  <b-form-input
                    id="invoice-website"
                    type="url"
                    placeholder="Nhập địa chỉ website"
                    required
                    v-model.trim="frmDataInvoice.invoiceWebsite"
                    :state="state('invoiceWebsite')"
                  />
                  <b-form-invalid-feedback :state="state('invoiceWebsite')">
                    {{ invalidFeedback('invoiceWebsite') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <div class="profile-form-actions">
              <b-button size="sm" variant="light" @click="closeForm('formInvoice')">
                Hủy
              </b-button>
              <b-button type="submit" size="sm" variant="primary" v-if="!buttonInfoInvoice">
                Cập nhật
              </b-button>
              <b-button type="button" size="sm" variant="secondary" disabled v-else>
                <b-spinner small type="grow"></b-spinner>
                Đang lưu...
              </b-button>
            </div>
          </b-form>
        </b-card>

        <!-- Thông tin hiển thị trên các hồ sơ gửi cơ quan thuế -->
        <b-card class="contact-info mt-3">
          <div class="title-contact">
            <h6 class="d-inline-block">Thông tin hiển thị trên các hồ sơ gửi cơ quan thuế</h6>
            <div class="edit-icon" v-show="!formContact">
              <span @click="editInfoContact"><i class="far fa-edit"></i></span>
            </div>
          </div>

          <!-- Chế độ xem -->
          <div class="contact-info-view" v-if="!formContact">
            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Họ và Tên:</b-col>
                  <b-col cols="8">{{ frmData.contactName }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Email:</b-col>
                  <b-col cols="8">{{ frmData.contactMail }}</b-col>
                </b-row>
              </b-col>
            </b-row>

            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Phone:</b-col>
                  <b-col cols="8">{{ frmData.contactPhone }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Địa chỉ:</b-col>
                  <b-col cols="8">{{ frmData.contactAddress }}</b-col>
                </b-row>
              </b-col>
            </b-row>
          </div>

          <!-- Chế độ sửa -->
          <b-form novalidate @submit.prevent="onSubmitContact" v-else>
            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Họ và Tên"
                  label-for="contact-name"
                  label-class="required"
                  :state="state('contactName')"
                >
                  <b-form-input
                    id="contact-name"
                    type="text"
                    placeholder="Nhập họ và tên"
                    v-model.trim="frmDataContact.contactName"
                    :state="state('contactName')"
                  />
                  <b-form-invalid-feedback :state="state('contactName')">
                    {{ invalidFeedback('contactName') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Email"
                  label-for="contact-email"
                  label-class="required"
                  :state="state('contactMail')"
                >
                  <b-form-input
                    id="contact-email"
                    type="email"
                    placeholder="Nhập địa chỉ email"
                    required
                    v-model.trim="frmDataContact.contactMail"
                    :state="state('contactMail')"
                  />
                  <b-form-invalid-feedback :state="state('contactMail')">
                    {{ invalidFeedback('contactMail') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Điện thoại"
                  label-for="contact-phone"
                  label-class="required"
                  :state="state('contactPhone')"
                >
                  <b-form-input
                    id="contact-phone"
                    type="tel"
                    placeholder="Nhập số điện thoại"
                    required
                    v-model.trim="frmDataContact.contactPhone"
                    :state="state('contactPhone')"
                  />
                  <b-form-invalid-feedback :state="state('contactPhone')">
                    {{ invalidFeedback('contactPhone') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Địa chỉ"
                  label-for="contact-address"
                  label-class="required"
                  :state="state('contactAddress')"
                >
                  <b-form-input
                    id="contact-address"
                    type="text"
                    placeholder="Nhập địa chỉ"
                    required
                    v-model.trim="frmDataContact.contactAddress"
                    :state="state('contactAddress')"
                  />
                  <b-form-invalid-feedback :state="state('contactAddress')">
                    {{ invalidFeedback('contactAddress') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <div class="profile-form-actions">
              <b-button size="sm" variant="light" @click="closeForm('formContact')">
                Hủy
              </b-button>
              <b-button type="submit" size="sm" variant="primary" v-if="!buttonContact">
                Cập nhật
              </b-button>
              <b-button type="button" size="sm" variant="secondary" disabled v-else>
                <b-spinner small type="grow"></b-spinner>
                Đang lưu...
              </b-button>
            </div>
          </b-form>
        </b-card>

        <!-- Thông tin thanh toán -->
        <b-card class="contact-info mt-3">
          <div class="title-contact">
            <h6 class="d-inline-block">Thông tin thanh toán</h6>
            <div class="edit-icon" v-show="!formBank">
              <span @click="editBank"><i class="far fa-edit"></i></span>
            </div>
          </div>

          <!-- Chế độ xem -->
          <div class="contact-info-view" v-if="!formBank">
            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="12">
                <b-row>
                  <b-col cols="2" class="font-weight-bold">Số tài khoản ngân hàng:</b-col>
                  <b-col cols="10">{{ frmData.bankNo }}</b-col>
                </b-row>
              </b-col>
            </b-row>

            <b-row class="pt-2 pb-2">
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Tên ngân hàng:</b-col>
                  <b-col cols="8">{{ frmData.bankName }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Chi nhánh:</b-col>
                  <b-col cols="8">{{ frmData.bankBrand || frmData.bankAddress }}</b-col>
                </b-row>
              </b-col>
            </b-row>
          </div>

          <!-- Chế độ sửa -->
          <b-form novalidate @submit.prevent="onSubmitBank" v-else>
            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Số tài khoản ngân hàng"
                  label-for="bank-no"
                  label-class="required"
                  :state="state('bankNo')"
                >
                  <b-form-input
                    id="bank-no"
                    type="text"
                    placeholder="Nhập số tài khoản ngân hàng"
                    v-model.trim="frmDataBank.bankNo"
                    :state="state('bankNo')"
                    required
                  />
                  <b-form-invalid-feedback :state="state('bankNo')">
                    {{ invalidFeedback('bankNo') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Tên ngân hàng"
                  label-for="bank-name"
                  label-class="required"
                  :state="state('bankName')"
                >
                  <v-select
                    id="bank-name"
                    v-model="frmDataBank.bankName"
                    :options="options.banks"
                    label="name"
                    placeholder="Chọn ngân hàng"
                    :reduce="(bank) => bank.abbreviation"
                    append-to-body
                    :calculate-position="withPopper"
                    :class="{ 'is-invalid': state('bankName') === false }"
                  />
                  <b-form-invalid-feedback :state="state('bankName')">
                    {{ invalidFeedback('bankName') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Chi nhánh"
                  label-for="bank-brand"
                  :state="state('bankBrand')"
                >
                  <b-form-input
                    id="bank-brand"
                    type="text"
                    placeholder="Nhập chi nhánh"
                    v-model.trim="frmDataBank.bankBrand"
                    :state="state('bankBrand')"
                  />
                  <b-form-invalid-feedback :state="state('bankBrand')">
                    {{ invalidFeedback('bankBrand') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <div class="profile-form-actions">
              <b-button size="sm" variant="light" @click="closeForm('formBank')">
                Hủy
              </b-button>
              <b-button type="submit" size="sm" variant="primary" v-if="!buttonBank">
                Cập nhật
              </b-button>
              <b-button type="button" size="sm" variant="secondary" disabled v-else>
                <b-spinner small type="grow"></b-spinner>
                Đang lưu...
              </b-button>
            </div>
          </b-form>
        </b-card>

        <!-- Thông tin đơn vị chủ quản -->
        <b-card class="contact-info mt-3">
          <div class="title-contact">
            <h6 class="d-inline-block">Thông tin đơn vị chủ quản</h6>
            <div class="edit-icon" v-show="!formTaxAuthority">
              <span @click="editTaxAuthority"><i class="far fa-edit"></i></span>
            </div>
          </div>

          <!-- Chế độ xem -->
          <div class="contact-info-view" v-if="!formTaxAuthority">
            <b-row class="pt-2 pb-2">
              <b-col cols="12" lg="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Cục thuế Tỉnh/Thành:</b-col>
                  <b-col cols="8">{{ frmData.bladeTaxAuthorityCity }}</b-col>
                </b-row>
              </b-col>
              <b-col cols="12" md="6">
                <b-row>
                  <b-col cols="4" class="font-weight-bold">Cơ quan thuế quản lý:</b-col>
                  <b-col cols="8">{{ frmData.bladeTaxAuthorityName }}</b-col>
                </b-row>
              </b-col>
            </b-row>
          </div>

          <!-- Chế độ sửa -->
          <b-form novalidate @submit.prevent="onSubmitTaxAuthority" v-else>
            <b-row>
              <b-col cols="12" md="6">
                <b-form-group
                  label="Cục thuế Tỉnh/Thành"
                  label-for="authority-city"
                  label-class="required"
                  :state="state('taxAuthorityCity')"
                >
                  <v-select
                    id="authority-city"
                    @input="getTaxAuthority"
                    v-model="frmTaxAuthority.taxAuthorityCity"
                    :options="options.taxAuthorities"
                    label="name"
                    placeholder="Chọn cục thuế Tỉnh/Thành"
                    :reduce="(item) => item.code"
                    append-to-body
                    :calculate-position="withPopper"
                    :class="{ 'is-invalid': state('taxAuthorityCity') === false }"
                  />
                  <b-form-invalid-feedback :state="state('taxAuthorityCity')">
                    {{ invalidFeedback('taxAuthorityCity') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>

              <b-col cols="12" md="6">
                <b-form-group
                  label="Cơ quan thuế quản lý"
                  label-for="authority-name"
                  label-class="required"
                  :state="state('taxAuthorityName')"
                >
                  <v-select
                    id="authority-name"
                    v-model="frmTaxAuthority.taxAuthorityName"
                    :options="options.taxAuthorityNames"
                    label="name"
                    placeholder="Chọn cơ quan thuế quản lý"
                    :reduce="(item) => item.code"
                    append-to-body
                    :calculate-position="withPopper"
                    :class="{ 'is-invalid': state('taxAuthorityName') === false }"
                  />
                  <b-form-invalid-feedback :state="state('taxAuthorityName')">
                    {{ invalidFeedback('taxAuthorityName') }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-row>

            <div class="profile-form-actions">
              <b-button size="sm" variant="light" @click="closeForm('formTaxAuthority')">
                Hủy
              </b-button>
              <b-button type="submit" size="sm" variant="primary" v-if="!buttonTaxAuthority">
                Cập nhật
              </b-button>
              <b-button type="button" size="sm" variant="secondary" disabled v-else>
                <b-spinner small type="grow"></b-spinner>
                Đang lưu...
              </b-button>
            </div>
          </b-form>
        </b-card>
      </b-container>
    </div>
  </div>
</template>

<script>
import Vue from "vue";
import axios from "@/plugins/axios";
import vSelect from "vue-select";
import "vue-select/dist/vue-select.css";
import moment from "moment";
import { createPopper as Popperjs } from "@popperjs/core";

export default {
  name: "Profile",
  components: { vSelect },
  data() {
    return {
      // Bật/tắt form
      formInfo: false,
      formRepresent: false,
      formInvoice: false,
      formContact: false,
      formBank: false,
      formTaxAuthority: false,

      // Cờ trạng thái tải
      buttonInfo: false,
      buttonRepresent: false,
      buttonInfoInvoice: false,
      buttonContact: false,
      buttonBank: false,
      buttonTaxAuthority: false,

      // Dữ liệu chính và model form
      frmData: {},
      frmInfo: {},
      frmRepresent: {},
      frmDataInvoice: {},
      frmDataContact: {},
      frmDataBank: {
        bankName: null,
        bankNo: null,
        bankAddress: null,
        bankBrand: null,
        bankNameCustom: null,
        bankSeparatorCharacter: null
      },
      frmTaxAuthority: {
        taxAuthorityCity: null,
        taxAuthorityName: null
      },

      // Dữ liệu tùy chọn
      options: {
        banks: [],
        taxAuthorities: [],
        taxAuthorityNames: [],
        gender: [
          { text: "Nam", value: 1 },
          { text: "Nữ", value: 0 }
        ]
      },

      // Validate và helper
      errors: {},
      logoName: null,
      faviconName: null,
      manualDate: ""
    };
  },

  mounted() {
    const vm = this;
    let canImport = true;

    axios.post("/setting/profile/ini", null, { meta: { suppressGlobalErrorToast: true } }).then((response) => {
      vm.options.banks = Array.isArray(response.data.banks)
        ? response.data.banks
        : [];
      vm.options.taxAuthorities = Array.isArray(response.data.taxAuthorities)
        ? response.data.taxAuthorities
        : [];
    }).catch((e) => {
      canImport = false;
      if (e?.response?.status === 403) {
        vm.$router.replace("/").catch(() => {});
      }
    }).finally(() => {
      if (canImport) {
        Vue.nextTick(() => {
          vm.importData();
        });
      }
    });
  },

  methods: {
    // Upload file
    onUploadLogo(e) {
      const files = e.target.files || e.dataTransfer.files;
      if (!files.length) return;
      this.frmInfo.logo = files[0];
      this.logoName = files[0].name;
    },

    onUploadFavicon(e) {
      const files = e.target.files || e.dataTransfer.files;
      if (!files.length) return;
      this.frmInfo.favicon = files[0];
      this.faviconName = files[0].name;
    },

    // Bật/tắt chỉnh sửa và nạp sẵn dữ liệu form
    editInfo() {
      this.clearScopedErrors(["companyBusiness", "companyName", "companyAddress", "logo", "favicon"]);
      this.frmInfo.companyName = this.frmData.companyName || "";
      this.frmInfo.companyAddress = this.frmData.companyAddress || "";
      this.frmInfo.companyBusiness = this.frmData.companyBusiness || "";
      this.frmInfo.logo = null;
      this.frmInfo.favicon = null;
      this.formInfo = true;
    },

    editRepresent() {
      this.clearScopedErrors([
        "representName",
        "representPhone",
        "representCitizenIdent",
        "representPassPort",
        "manualDate",
        "representGender",
        "representMail"
      ]);
      this.frmRepresent.representName = this.frmData.representName || "";
      this.frmRepresent.representPhone = this.frmData.representPhone || "";
      this.frmRepresent.representCitizenIdent =
        this.frmData.representCitizenIdent || "";
      this.frmRepresent.representPassPort = this.frmData.representPassPort || "";
      this.frmRepresent.representDateBirth =
        this.frmData.representDateBirth || null;
      this.frmRepresent.representGender = this.frmData.representGender;
      this.frmRepresent.representMail = this.frmData.representMail || "";
      this.formRepresent = true;

      if (this.frmRepresent.representDateBirth) {
        this.syncManualDate(this.frmRepresent.representDateBirth);
      }
    },

    editInfoInvoice() {
      this.clearScopedErrors(["invoiceEmail", "invoicePhone", "invoiceFax", "invoiceWebsite"]);
      this.frmDataInvoice.invoiceEmail = this.frmData.invoiceEmail || "";
      this.frmDataInvoice.invoicePhone = this.frmData.invoicePhone || "";
      this.frmDataInvoice.invoiceFax = this.frmData.invoiceFax || "";
      this.frmDataInvoice.invoiceWebsite = this.frmData.invoiceWebsite || "";
      this.formInvoice = true;
    },

    editInfoContact() {
      this.clearScopedErrors(["contactName", "contactMail", "contactPhone", "contactAddress"]);

      this.frmDataContact.contactName = this.frmData.contactName || ""
      this.frmDataContact.contactMail = this.frmData.contactMail || ""
      this.frmDataContact.contactPhone = this.frmData.contactPhone || ""
      this.frmDataContact.contactAddress = this.frmData.contactAddress || ""
      this.formContact = true
    },

    editBank() {
      this.clearScopedErrors(["bankNo", "bankName", "bankBrand"]);
      this.frmDataBank.bankName = this.frmData.bankAbbreviation || null;
      this.frmDataBank.bankNo = this.frmData.bankNo || null;
      this.frmDataBank.bankAddress = this.frmData.bankAddress || null;
      this.frmDataBank.bankBrand = this.frmData.bankBrand || null;
      this.frmDataBank.bankNameCustom = this.frmData.bankNameCustom || null;
      this.frmDataBank.bankSeparatorCharacter =
        this.frmData.bankSeparatorCharacter || null;
      this.formBank = true;
    },

    editTaxAuthority() {
      this.clearScopedErrors(["taxAuthorityCity", "taxAuthorityName"]);
      this.frmTaxAuthority.taxAuthorityCity = this.frmData.taxAuthorityCity || null;
      this.frmTaxAuthority.taxAuthorityName = this.frmData.taxAuthorityName || null;
      this.formTaxAuthority = true;
    },

    // Tải tên cơ quan thuế theo mã tỉnh/thành
    getTaxAuthority(code) {
      const vm = this;
      vm.options.taxAuthorityNames = [];
      vm.frmTaxAuthority.taxAuthorityName = null;

      const parsedCode = parseInt(code, 10) || 0;
      if (parsedCode > 0) {
        axios
          .post("/setting/profile/get-tax-authority", { parentCode: parsedCode }, { meta: { suppressGlobalErrorToast: true } })
          .then((response) => {
            if (response.data === 0) {
              vm.frmData.taxAuthorityCity = vm.frmTaxAuthority.taxAuthorityCity = null;
              vm.frmData.taxAuthorityName = vm.frmTaxAuthority.taxAuthorityName = null;
              return;
            }
            if (!response.data || Object.keys(response.data).length < 1) {
              vm.frmData.taxAuthorityName = vm.frmTaxAuthority.taxAuthorityName = null;
              return;
            }
            vm.options.taxAuthorityNames = response.data;
          })
          .catch(() => {});
      }
      return false;
    },

    // Đóng form
    closeForm(value) {
      switch (value) {
        case "formInfo":
          this.formInfo = false;
          this.frmInfo = {};
          this.clearScopedErrors(["companyBusiness", "companyName", "companyAddress", "logo", "favicon"]);
          break;
        case "formRepresent":
          this.formRepresent = false;
          this.frmRepresent = {};
          this.clearScopedErrors([
            "representName",
            "representPhone",
            "representCitizenIdent",
            "representPassPort",
            "manualDate",
            "representGender",
            "representMail"
          ]);
          break;
        case "formInvoice":
          this.formInvoice = false;
          this.frmDataInvoice = {};
          this.clearScopedErrors(["invoiceEmail", "invoicePhone", "invoiceFax", "invoiceWebsite"]);
          break;
        case "formContact":
          this.formContact = false
          this.frmDataContact = {}
          this.clearScopedErrors(["contactName", "contactMail", "contactPhone", "contactAddress"]);
          break;
        case "formBank":
          this.formBank = false;
          this.frmDataBank = {
            bankName: null,
            bankNo: null,
            bankAddress: null,
            bankBrand: null,
            bankNameCustom: null,
            bankSeparatorCharacter: null
          };
          this.clearScopedErrors(["bankNo", "bankName", "bankBrand"]);
          break;
        case "formTaxAuthority":
          this.formTaxAuthority = false;
          this.frmTaxAuthority = {
            taxAuthorityCity: null,
            taxAuthorityName: null
          };
          this.clearScopedErrors(["taxAuthorityCity", "taxAuthorityName"]);
          break;
        default:
      }
      return false;
    },

    // Import dữ liệu chính
    importData() {
      axios
        .post("/setting/profile/get", null, { meta: { suppressGlobalErrorToast: true } })
        .then((response) => {
          this.frmData = response.data || {};
          if (this.frmData.taxAuthorityCity) {
            this.getTaxAuthority(this.frmData.taxAuthorityCity);
          }
        })
        .catch(() => {
          this.$router.push("/");
        });
    },

    trimValue(value) {
      return value == null ? "" : String(value).trim();
    },

    isBlank(value) {
      return this.trimValue(value) === "";
    },

    setScopedErrors(fields, errors) {
      const next = { ...this.errors };
      fields.forEach((field) => delete next[field]);
      this.errors = { ...next, ...errors };
      return Object.keys(errors).length === 0;
    },

    clearScopedErrors(fields) {
      const next = { ...this.errors };
      fields.forEach((field) => delete next[field]);
      this.errors = next;
    },

    addRequiredError(errors, field, value, label) {
      if (this.isBlank(value)) {
        errors[field] = [`Vui lòng nhập ${label}`];
      }
    },

    isValidEmail(value) {
      const email = this.trimValue(value);
      if (!email) return false;
      return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email);
    },

    isValidPhone(value) {
      const phone = this.trimValue(value).replace(/[\s().-]/g, "");
      return /^(0\d{9,10}|\+?\d{9,15})$/.test(phone);
    },

    isValidCitizenIdent(value) {
      const ident = this.trimValue(value);
      return ident === "" || /^(\d{9}|\d{12})$/.test(ident);
    },

    isValidPassport(value) {
      const passport = this.trimValue(value);
      return passport === "" || /^[A-Za-z0-9]{6,20}$/.test(passport);
    },

    isValidWebsite(value) {
      const website = this.trimValue(value);
      if (!website || /\s/.test(website)) return false;
      try {
        const url = new URL(/^https?:\/\//i.test(website) ? website : `https://${website}`);
        return !!url.hostname && url.hostname.includes(".");
      } catch {
        return false;
      }
    },

    isValidBankNo(value) {
      const bankNo = this.trimValue(value);
      return /^[0-9A-Za-z.-]{4,32}$/.test(bankNo);
    },

    isValidImageFile(file) {
      if (!file) return true;
      return /\.(png|jpe?g)$/i.test(file.name || "");
    },

    isValidPastOrTodayDate(value) {
      if (!value) return false;
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) return false;
      const today = new Date();
      today.setHours(23, 59, 59, 999);
      return date >= new Date("1900-01-01") && date <= today;
    },

    parseManualDateValue(value) {
      const text = this.trimValue(value);
      const match = text.match(/^(0[1-9]|[12][0-9]|3[01])\/(0[1-9]|1[0-2])\/(19\d{2}|20\d{2})$/);
      if (!match) return null;
      const day = Number(match[1]);
      const month = Number(match[2]);
      const year = Number(match[3]);
      const parsed = new Date(year, month - 1, day);
      if (
        parsed.getFullYear() !== year ||
        parsed.getMonth() !== month - 1 ||
        parsed.getDate() !== day
      ) {
        return null;
      }
      return parsed;
    },

    validateInfoForm() {
      const fields = ["companyBusiness", "companyName", "companyAddress", "logo", "favicon"];
      const errors = {};
      this.addRequiredError(errors, "companyBusiness", this.frmInfo.companyBusiness, "ngành nghề");
      this.addRequiredError(errors, "companyName", this.frmInfo.companyName, "tên đơn vị");
      this.addRequiredError(errors, "companyAddress", this.frmInfo.companyAddress, "địa chỉ");
      if (!this.isValidImageFile(this.frmInfo.logo)) {
        errors.logo = ["Logo chỉ hỗ trợ PNG, JPG hoặc JPEG"];
      }
      if (!this.isValidImageFile(this.frmInfo.favicon)) {
        errors.favicon = ["Favicon chỉ hỗ trợ PNG, JPG hoặc JPEG"];
      }
      return this.setScopedErrors(fields, errors);
    },

    validateRepresentForm() {
      const fields = [
        "representName",
        "representPhone",
        "representCitizenIdent",
        "representPassPort",
        "manualDate",
        "representGender",
        "representMail"
      ];
      const errors = {};
      this.addRequiredError(errors, "representName", this.frmRepresent.representName, "họ và tên");
      this.addRequiredError(errors, "representPhone", this.frmRepresent.representPhone, "số điện thoại");
      if (!errors.representPhone && !this.isValidPhone(this.frmRepresent.representPhone)) {
        errors.representPhone = ["Số điện thoại không hợp lệ"];
      }
      if (!this.isValidCitizenIdent(this.frmRepresent.representCitizenIdent)) {
        errors.representCitizenIdent = ["Căn cước công dân phải gồm 9 hoặc 12 chữ số"];
      }
      if (!this.isValidPassport(this.frmRepresent.representPassPort)) {
        errors.representPassPort = ["Số hộ chiếu chỉ gồm chữ và số, từ 6 đến 20 ký tự"];
      }
      const manualDate = this.parseManualDateValue(this.manualDate);
      if (!manualDate || !this.isValidPastOrTodayDate(manualDate)) {
        errors.manualDate = ["Ngày sinh không hợp lệ"];
      } else {
        this.frmRepresent.representDateBirth = manualDate;
      }
      if (this.frmRepresent.representGender !== 0 && this.frmRepresent.representGender !== 1) {
        errors.representGender = ["Vui lòng chọn giới tính"];
      }
      this.addRequiredError(errors, "representMail", this.frmRepresent.representMail, "email");
      if (!errors.representMail && !this.isValidEmail(this.frmRepresent.representMail)) {
        errors.representMail = ["Email không hợp lệ"];
      }
      return this.setScopedErrors(fields, errors);
    },

    validateInvoiceForm() {
      const fields = ["invoiceEmail", "invoicePhone", "invoiceFax", "invoiceWebsite"];
      const errors = {};
      this.addRequiredError(errors, "invoiceEmail", this.frmDataInvoice.invoiceEmail, "email");
      if (!errors.invoiceEmail && !this.isValidEmail(this.frmDataInvoice.invoiceEmail)) {
        errors.invoiceEmail = ["Email không hợp lệ"];
      }
      this.addRequiredError(errors, "invoicePhone", this.frmDataInvoice.invoicePhone, "số điện thoại");
      if (!errors.invoicePhone && !this.isValidPhone(this.frmDataInvoice.invoicePhone)) {
        errors.invoicePhone = ["Số điện thoại không hợp lệ"];
      }
      this.addRequiredError(errors, "invoiceFax", this.frmDataInvoice.invoiceFax, "số fax");
      if (!errors.invoiceFax && !/^[0-9+().\-\s]{6,20}$/.test(this.trimValue(this.frmDataInvoice.invoiceFax))) {
        errors.invoiceFax = ["Số fax không hợp lệ"];
      }
      this.addRequiredError(errors, "invoiceWebsite", this.frmDataInvoice.invoiceWebsite, "website");
      if (!errors.invoiceWebsite && !this.isValidWebsite(this.frmDataInvoice.invoiceWebsite)) {
        errors.invoiceWebsite = ["Website không hợp lệ"];
      }
      return this.setScopedErrors(fields, errors);
    },

    validateContactForm() {
      const fields = ["contactName", "contactMail", "contactPhone", "contactAddress"];
      const errors = {};
      this.addRequiredError(errors, "contactName", this.frmDataContact.contactName, "họ và tên");
      this.addRequiredError(errors, "contactMail", this.frmDataContact.contactMail, "email");
      if (!errors.contactMail && !this.isValidEmail(this.frmDataContact.contactMail)) {
        errors.contactMail = ["Email không hợp lệ"];
      }
      this.addRequiredError(errors, "contactPhone", this.frmDataContact.contactPhone, "số điện thoại");
      if (!errors.contactPhone && !this.isValidPhone(this.frmDataContact.contactPhone)) {
        errors.contactPhone = ["Số điện thoại không hợp lệ"];
      }
      this.addRequiredError(errors, "contactAddress", this.frmDataContact.contactAddress, "địa chỉ");
      return this.setScopedErrors(fields, errors);
    },

    validateBankForm() {
      const fields = ["bankNo", "bankName", "bankBrand"];
      const errors = {};
      this.addRequiredError(errors, "bankNo", this.frmDataBank.bankNo, "số tài khoản ngân hàng");
      if (!errors.bankNo && !this.isValidBankNo(this.frmDataBank.bankNo)) {
        errors.bankNo = ["Số tài khoản ngân hàng không hợp lệ"];
      }
      if (this.isBlank(this.frmDataBank.bankName)) {
        errors.bankName = ["Vui lòng chọn tên ngân hàng"];
      }
      if (!this.isBlank(this.frmDataBank.bankBrand) && this.trimValue(this.frmDataBank.bankBrand).length > 255) {
        errors.bankBrand = ["Chi nhánh không được vượt quá 255 ký tự"];
      }
      return this.setScopedErrors(fields, errors);
    },

    validateTaxAuthorityForm() {
      const fields = ["taxAuthorityCity", "taxAuthorityName"];
      const errors = {};
      if (this.frmTaxAuthority.taxAuthorityCity == null || this.frmTaxAuthority.taxAuthorityCity === "") {
        errors.taxAuthorityCity = ["Vui lòng chọn cục thuế Tỉnh/Thành"];
      }
      if (this.frmTaxAuthority.taxAuthorityName == null || this.frmTaxAuthority.taxAuthorityName === "") {
        errors.taxAuthorityName = ["Vui lòng chọn cơ quan thuế quản lý"];
      }
      return this.setScopedErrors(fields, errors);
    },

    // Hàm xử lý submit
    onSubmitInfo(e) {
      e.preventDefault();
      if (!this.validateInfoForm()) return;
      this.buttonInfo = true;

      const frmData = new FormData();
      frmData.append("companyName", this.trimValue(this.frmInfo.companyName));
      frmData.append("companyAddress", this.trimValue(this.frmInfo.companyAddress));
      frmData.append("companyBusiness", this.trimValue(this.frmInfo.companyBusiness));
      if (this.frmInfo.logo) frmData.append("logo", this.frmInfo.logo);
      if (this.frmInfo.favicon) frmData.append("favicon", this.frmInfo.favicon);

      axios
        .post("/setting/profile/update-info", frmData, {
          headers: { "Content-Type": "multipart/form-data" }
        })
        .then(async () => {
          this.buttonInfo = false;
          this.formInfo = false;
          this.frmInfo = {};
          this.importData();
          
          // Tải lại thông tin user từ server để lấy dữ liệu công ty mới nhất
          try {
            const infoRes = await axios.get('/auth/info', { meta: { suppressGlobalErrorToast: true } });
            if (infoRes.data) {
              // Cập nhật state app toàn cục bằng dữ liệu mới
              this.$app.info = infoRes.data;
            }
          } catch (error) {
            console.warn('Failed to refresh auth info:', error);
            // Dự phòng gọi loadAppInfo nếu /auth/info lỗi
            this.$app.loadAppInfo();
          }
        })
        .catch((errors) => {
          this.errors = (errors.response && errors.response.data && errors.response.data.errors) || {};
          this.buttonInfo = false;
        });
    },

    onsubmitRepresent(e) {
      e.preventDefault();
      if (!this.validateRepresentForm()) return;
      this.buttonRepresent = true;
      const payload = {
        ...this.frmRepresent,
        representName: this.trimValue(this.frmRepresent.representName),
        representPhone: this.trimValue(this.frmRepresent.representPhone),
        representCitizenIdent: this.trimValue(this.frmRepresent.representCitizenIdent),
        representPassPort: this.trimValue(this.frmRepresent.representPassPort),
        representMail: this.trimValue(this.frmRepresent.representMail),
        representDateBirth: moment(this.frmRepresent.representDateBirth).format("YYYY-MM-DD")
      };

      axios
        .post("/setting/profile/update-represent", payload)
        .then(() => {
          this.buttonRepresent = false;
          this.formRepresent = false;
          this.frmRepresent = {};
          this.importData();
        })
        .catch((errors) => {
          this.errors = (errors.response && errors.response.data && errors.response.data.errors) || {};
          this.buttonRepresent = false;
        });
    },

    onSubmitInfoInvoice(e) {
      e.preventDefault();
      if (!this.validateInvoiceForm()) return;
      this.buttonInfoInvoice = true;
      const payload = {
        invoiceEmail: this.trimValue(this.frmDataInvoice.invoiceEmail),
        invoicePhone: this.trimValue(this.frmDataInvoice.invoicePhone),
        invoiceFax: this.trimValue(this.frmDataInvoice.invoiceFax),
        invoiceWebsite: this.trimValue(this.frmDataInvoice.invoiceWebsite)
      };

      axios
        .post("/setting/profile/update-info-invoice", payload)
        .then(() => {
          this.buttonInfoInvoice = false;
          this.formInvoice = false;
          this.frmDataInvoice = {};
          this.importData();
        })
        .catch((errors) => {
          this.errors = (errors.response && errors.response.data && errors.response.data.errors) || {};
          this.buttonInfoInvoice = false;
        });
    },

    onSubmitContact(e) {
      e.preventDefault();
      if (!this.validateContactForm()) return;
      this.buttonContact = true;
      const payload = {
        contactName: this.trimValue(this.frmDataContact.contactName),
        contactMail: this.trimValue(this.frmDataContact.contactMail),
        contactPhone: this.trimValue(this.frmDataContact.contactPhone),
        contactAddress: this.trimValue(this.frmDataContact.contactAddress)
      };

      axios
        .post("/setting/profile/update-contact", payload)
        .then(() => {
          this.buttonContact = false;
          this.formContact = false;
          this.frmDataContact = {};
          this.importData();
        })
        .catch((errors) => {
          this.errors = (errors.response && errors.response.data && errors.response.data.errors) || {};
          this.buttonContact = false;
        });
    },

    onSubmitBank(e) {
      e.preventDefault();
      if (!this.validateBankForm()) return;
      this.buttonBank = true;
      const payload = {
        ...this.frmDataBank,
        bankNo: this.trimValue(this.frmDataBank.bankNo),
        bankBrand: this.trimValue(this.frmDataBank.bankBrand)
      };

      axios
        .post("/setting/profile/update-bank", payload)
        .then(() => {
          this.buttonBank = false;
          this.closeForm("formBank");
          this.importData();
        })
        .catch((errors) => {
          this.errors = (errors.response && errors.response.data && errors.response.data.errors) || {};
          this.buttonBank = false;
        });
    },

    onSubmitTaxAuthority(e) {
      e.preventDefault();
      if (!this.validateTaxAuthorityForm()) return;
      this.buttonTaxAuthority = true;

      axios
        .post("/setting/profile/update-tax-authority", this.frmTaxAuthority)
        .then(() => {
          this.buttonTaxAuthority = false;
          this.closeForm("formTaxAuthority");
          this.importData();
        })
        .catch((errors) => {
          this.errors = (errors.response && errors.response.data && errors.response.data.errors) || {};
          this.buttonTaxAuthority = false;
        });
    },

    // Hàm hỗ trợ validate
    state(field) {
      const errors = this.errors || {};
      if (!Object.prototype.hasOwnProperty.call(errors, field)) {
        return null; // trung lập
      }
      return false; // không hợp lệ
    },

    invalidFeedback(field) {
      const errors = this.errors || {};
      if (!Object.prototype.hasOwnProperty.call(errors, field)) {
        return "";
      }
      const value = errors[field] || [];
      return Array.isArray(value) ? value.join(" ") : String(value);
    },

    // Định vị menu xổ xuống của v-select.
    withPopper(dropdownList, component, { width }) {
      dropdownList.style.width = width;
      const popper = Popperjs(component.$refs.toggle, dropdownList, {
        placement: "bottom",
        modifiers: [
          {
            name: "offset",
            options: {
              offset: [0, -1]
            }
          },
          {
            name: "toggleClass",
            enabled: true,
            phase: "write",
            fn({ state }) {
              component.$el.classList.toggle("drop-up", state.placement === "top");
            }
          }
        ]
      });
      return () => popper.destroy();
    },

    // Xử lý ngày nhập tay và đồng bộ datepicker
    parseManualDate() {
      // Reset lỗi trường trước
      const newErrors = { ...this.errors };
      delete newErrors.manualDate;
      this.errors = newErrors;

      const parsed = this.parseManualDateValue(this.manualDate);
      if (!parsed || !this.isValidPastOrTodayDate(parsed)) {
        this.errors = {
          ...this.errors,
          manualDate: ["Ngày không hợp lệ. Vui lòng nhập đúng định dạng dd/mm/yyyy"]
        };
        return;
      }

      this.frmRepresent.representDateBirth = parsed;

      if (this.frmRepresent.representDateBirth) {
        this.syncManualDate(this.frmRepresent.representDateBirth);
      }
    },

    syncManualDate(date) {
      const parsed = new Date(date);
      if (!isNaN(parsed)) {
        const day = String(parsed.getDate()).padStart(2, "0");
        const month = String(parsed.getMonth() + 1).padStart(2, "0");
        const year = parsed.getFullYear();
        this.manualDate = `${day}/${month}/${year}`;

        // Xóa lỗi cho manualDate
        const newErrors = { ...this.errors };
        delete newErrors.manualDate;
        this.errors = newErrors;
      } else {
        this.manualDate = "";
        this.errors = {
          ...this.errors,
          manualDate: ["Ngày không hợp lệ."]
        };
      }
    },

    formatter(value) {
      if (value) {
        return moment(value).format("DD/MM/YYYY");
      }
      return "";
    }
  }
};
</script>

<style scoped>
.contact-info {
  border: 1px solid #eaeaea;
  border-radius: 6px;
  padding: 12px;
}

.title-contact {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.edit-icon {
  cursor: pointer;
}

.profile-form-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 8px;
}

.profile-form-actions .btn {
  min-width: 82px;
}

.profile-form-actions .btn-primary {
  background: #2563eb;
  border-color: #2563eb;
}

.profile-form-actions .btn-secondary {
  align-items: center;
  display: inline-flex;
  gap: 6px;
  justify-content: center;
}

.profile-upload-field {
  background: #fff;
  border: 1px solid #d9e2ef;
  border-radius: 8px;
  padding: 12px;
}

.v-select {
  width: 100%;
}

::v-deep .v-select.is-invalid .vs__dropdown-toggle {
  border-color: #dc3545;
}

::v-deep .v-select.is-invalid .vs__dropdown-toggle:focus-within {
  box-shadow: 0 0 0 .2rem rgba(220, 53, 69, .25);
}

@media (max-width: 576px) {
  .profile-form-actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .profile-form-actions .btn {
    width: 100%;
  }
}
</style>
