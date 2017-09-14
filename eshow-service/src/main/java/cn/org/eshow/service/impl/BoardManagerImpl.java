package cn.org.eshow.service.impl;

import cn.org.eshow.bean.query.BoardQuery;
import cn.org.eshow.common.page.Page;
import cn.org.eshow.dao.BoardDao;
import cn.org.eshow.model.Board;
import cn.org.eshow.service.BoardManager;
import cn.org.eshow.service.impl.GenericManagerImpl;

import java.util.List;
import javax.jws.WebService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@WebService(serviceName = "BoardService", endpointInterface = "cn.org.eshow.service.BoardManager")
@Service
public class BoardManagerImpl extends GenericManagerImpl<Board, Integer> implements BoardManager {

    @Autowired
    BoardDao boardDao;

    @Autowired
    public BoardManagerImpl(BoardDao boardDao) {
        super(boardDao);
        this.boardDao = boardDao;
    }

    @Override
    public List<Board> list(BoardQuery query) {
        return boardDao.list(query);
    }

    @Override
    public Page<Board> search(BoardQuery query) {
        return boardDao.search(query);
    }

    @Override
    public Board updateBoard(Board old, Board board) {
        old.setName(board.getName() == null ? old.getName() : board.getName());
        old.setDescription(board.getDescription() == null ? old.getDescription() : board.getDescription());
        return boardDao.save(old);
    }

    @Override
    public Board updateEnabled(Board board) {
        board.setEnabled(!board.getEnabled());//反向设置状态
        return boardDao.save(board);
    }
}