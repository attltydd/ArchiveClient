package client.ui;

import client.model.FileItem;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FileTableModel extends AbstractTableModel {

    private final List<FileItem> files = new ArrayList<>();
    private final String[] columns = {
        "Имя",
        "Размер (байт)"
    };

    public void setFiles(List<FileItem> newFiles) {
        files.clear();
        files.addAll(newFiles);
        
        files.sort((a, b) -> Long.compare(b.getSize(), a.getSize()));
        
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return files.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        FileItem file = files.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return file.getName();
            case 1:
                return file.getSize();
            default:
                return "";
        }
    }
}