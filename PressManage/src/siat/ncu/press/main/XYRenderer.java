package siat.ncu.press.main;

import org.achartengine.renderer.XYMultipleSeriesRenderer;

public class XYRenderer extends XYMultipleSeriesRenderer
{
    //ͼ����ʽ������
    public XYRenderer() {

    }

    public XYRenderer(String title, String xTitle, String yTitle, double xMin, double xMax, double yMin, double yMax,
            int axesColor, int labelsColor, int XTitleTextSize, int ChartTitleTextSize, int LabelsTextSize,
            int LegendTextSize, int Xshow, int Yshow, boolean ShowGrid) {
        this.setXTitle(xTitle);// X�����
        this.setYTitle(yTitle);// Y�����
        this.setXAxisMin(xMin);// X��Сֵ
        this.setXAxisMax(xMax);// X���ֵ
        this.setYAxisMin(yMin);// Y��Сֵ
        this.setYAxisMax(yMax);// Y��Сֵ
        this.setAxesColor(axesColor);// X����ɫ
        this.setLabelsColor(labelsColor);// Y����ɫ
        this.setAxisTitleTextSize(XTitleTextSize); // ��������������С��16
        this.setChartTitleTextSize(ChartTitleTextSize); // ͼ�����������С��20
        this.setLabelsTextSize(LabelsTextSize); // ���ǩ�����С��15
        this.setLegendTextSize(LegendTextSize); // ͼ�������С��15
        this.setXLabels(Xshow);//����X����ʾ�Ŀ̶ȱ�ǩ�ĸ���
        this.setYLabels(Yshow);//����Y����ʾ�Ŀ̶ȱ�ǩ�ĸ���
        this.setShowGrid(ShowGrid); // ����������ʾ
    }

}