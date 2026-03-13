import { get, doLogin } from '../../api/request';

// 格式化当前日期为类似 "2024年8月20日 星期一"
function getFormattedDate() {
  const date = new Date();
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
  const weekDay = days[date.getDay()];
  return `${year}年${month}月${day}日 ${weekDay}`;
}

Page({
  data: {
    stats: {
      totalAdvanceAmount: 0.00,
      pendingAdvanceAmount: 0.00,
      recoveredAdvanceAmount: 0.00,
      totalIncomeAmount: 0.00,
      receivedIncomeAmount: 0.00,
      pendingIncomeAmount: 0.00,
      totalOrders: 0,
      completedOrders: 0,
      inProgressOrders: 0
    },
    currentDate: getFormattedDate(),
    // 根据预计收入扣减总垫付，模拟净利润（可根据实际后端数值修改）
    netProfit: '0.00'
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().init();
    }

    const token = wx.getStorageSync('Authorization');
    if (token) {
      this.fetchDashboardStats();
    } else {
      doLogin().then(() => {
        this.fetchDashboardStats();
      });
    }
  },

  onPullDownRefresh() {
    this.fetchDashboardStats().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  fetchDashboardStats() {
    return get('/dashboard/stats').then(res => {
      if (res) {
        // 简单计算当前净利润（总预计收入 - 未收回垫付成本等，视业务而定）
        // 这里只是基于 stats.totalIncomeAmount 假设一个计算：
        const profit = parseFloat(res.totalIncomeAmount || 0) - parseFloat(res.pendingAdvanceAmount || 0);

        this.setData({
          stats: res,
          netProfit: profit > 0 ? profit.toFixed(2) : '0.00'
        });
      }
    });
  }
});
