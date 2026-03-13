import { post, doLogin } from '../../api/request';
import Toast from 'tdesign-miniprogram/toast/index';

Page({
    data: {
        hasLoggedIn: false, // 是否已经登录
        userInfo: {
            avatarUrl: 'https://tdesign.gtimg.com/miniprogram/images/avatar1.png', // 默认头像
            nickName: '微信用户',
        },
    },

    onLoad() {
        this.checkLoginStatus();
    },

    onShow() {
        this.checkLoginStatus();
        if (typeof this.getTabBar === 'function' && this.getTabBar()) {
            this.getTabBar().init();
        }
    },

    // 检查本地 Token 是否存在
    checkLoginStatus() {
        const token = wx.getStorageSync('Authorization');
        if (token) {
            this.setData({ hasLoggedIn: true });
        } else {
            this.setData({ hasLoggedIn: false });
            // 无感自动登录
            doLogin().then(() => {
                this.setData({ hasLoggedIn: true });
                Toast({ context: this, selector: '#t-toast', message: '已自动登录', theme: 'success' });
            }).catch(() => {
                Toast({ context: this, selector: '#t-toast', message: '自动登录失败', theme: 'error' });
            });
        }
    },

    // 点击登录按钮 (保留作为手动重试入口)
    onLogin() {
        this.checkLoginStatus();
    },

    // 退出登录
    onLogout() {
        wx.showModal({
            title: '提示',
            content: '确定要退出登录吗？',
            success: (res) => {
                if (res.confirm) {
                    wx.showLoading({ title: '退出中...' });
                    post('/wx/logout').then(() => {
                        wx.removeStorageSync('Authorization');
                        this.setData({ hasLoggedIn: false });
                        Toast({ context: this, selector: '#t-toast', message: '已退出登录' });
                    }).finally(() => {
                        wx.hideLoading();
                    });

                    // 兜底清理（防止后端网络异常导致前端卡在登录态）
                    wx.removeStorageSync('Authorization');
                    this.setData({ hasLoggedIn: false });
                }
            }
        });
    }
});
