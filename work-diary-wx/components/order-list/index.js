// 将 ISO 时间字符串格式化为 "3月06日 13:49"
function formatTime(isoStr) {
    if (!isoStr) return '';
    const str = String(isoStr).replace('T', ' ');
    const d = new Date(str);
    if (isNaN(d.getTime())) return isoStr;
    const month = d.getMonth() + 1;
    const day = d.getDate();
    const h = String(d.getHours()).padStart(2, '0');
    const m = String(d.getMinutes()).padStart(2, '0');
    return `${month}月${day}日 ${h}:${m}`;
}

Component({
    properties: {
        orderList: {
            type: Array,
            value: []
        },
        loading: {
            type: Boolean,
            value: false
        },
        hasMore: {
            type: Boolean,
            value: true
        }
    },

    observers: {
        // 每次 orderList 改变时，预处理时间字段
        'orderList': function (list) {
            const formatted = (list || []).map(item => Object.assign({}, item, {
                formattedTime: formatTime(item.createTime)
            }));
            this.setData({ formattedList: formatted });
        }
    },

    data: {
        formattedList: []
    },

    methods: {
        onActionTap(e) {
            const { action, item } = e.currentTarget.dataset;
            this.triggerEvent('action', { action, item });
        },
        onOrderDetail(e) {
            const id = e.currentTarget.dataset.id;
            // TODO: 跳转到详情页
            wx.navigateTo({
                url: `/pages/release/index?id=${id}`
            });
        }
    }
});
