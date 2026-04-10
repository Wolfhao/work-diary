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

Component({
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
    netProfit: '0.00'
  },

  lifetimes: {
    attached() {
      this.init();
    }
  },

  methods: {
    init() {
      const token = wx.getStorageSync('Authorization');
      if (token) {
        this.fetchDashboardStats();
      } else {
        doLogin().then(() => {
          this.fetchDashboardStats();
        });
      }
    },

    fetchDashboardStats() {
      return get('/dashboard/stats').then(res => {
        if (res) {
          const profit = parseFloat(res.totalIncomeAmount || 0) - parseFloat(res.pendingAdvanceAmount || 0);
          this.setData({
            stats: res,
            netProfit: profit > 0 ? profit.toFixed(2) : '0.00'
          });
        }
      });
    }
  }
});
