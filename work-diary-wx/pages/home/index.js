import { get, doLogin } from '../../api/request';

Page({
    data: {
        pendingCount: 0
    },

    onLoad() {
        // onShow 首次也会触发，无需在 onLoad 重复请求
    },

    onShow() {
        const token = wx.getStorageSync('Authorization');
        if (token) {
            this.loadPendingCount();
        } else {
            // token 还没就绪（app.js 的 doLogin 还在飞），复用同一个 Promise 等它完成再请求
            doLogin().then(() => this.loadPendingCount()).catch(() => {});
        }
    },

    loadPendingCount() {
        get('/dashboard/stats')
            .then(res => {
                this.setData({ pendingCount: (res && res.inProgressOrders) || 0 });
            })
            .catch(() => {});
    },

    onWorkOrderTap() {
        wx.navigateTo({
            url: '/modules/workorder/index'
        });
    },

    onTravelTap() {
        wx.navigateTo({
            url: '/modules/travel/index'
        });
    },

    onAccountTap() {
        wx.showToast({
            title: '开发中...',
            icon: 'none'
        });
    },

    onMoreTap() {
        wx.showToast({
            title: '敬请期待',
            icon: 'none'
        });
    }
});
