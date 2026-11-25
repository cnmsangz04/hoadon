<template>
  <stock-issue-list
    :stock-issue-type="1"
    page-title="Danh sách phiếu xuất kho kiêm vận chuyển nội bộ"
    receiver-label="Đơn vị nhận"
    create-route="/stock-issue/internal-transport/create"
    edit-route-prefix="/stock-issue/internal-transport"
    report-route="/reports/stock-issue/internal-transport/list"
  />
</template>

<script>
import StockIssueList from '@/views/customers/stock-issue/list.vue'

export default {
  name: 'StockIssueInternalTransportList',
  components: { StockIssueList },
}
</script>
