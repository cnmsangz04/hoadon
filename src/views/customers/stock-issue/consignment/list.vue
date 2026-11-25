<template>
  <stock-issue-list
    :stock-issue-type="2"
    page-title="Danh sách phiếu xuất kho hàng gửi bán đại lý"
    receiver-label="Đại lý nhận"
    create-route="/stock-issue/consignment/create"
    edit-route-prefix="/stock-issue/consignment"
    report-route="/reports/stock-issue/consignment/list"
  />
</template>

<script>
import StockIssueList from '@/views/customers/stock-issue/list.vue'

export default {
  name: 'StockIssueConsignmentList',
  components: { StockIssueList },
}
</script>
