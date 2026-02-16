import { useRef, useEffect, useCallback, useState } from 'react';

interface RoomNode {
  id: string;
  name: string;
  x: number;
  y: number;
}

interface ExitEdge {
  fromRoomId: string;
  toRoomId: string;
  direction: string;
}

interface MapCanvasProps {
  rooms: RoomNode[];
  exits: ExitEdge[];
  selectedRoomId: string | null;
  onSelectRoom: (id: string) => void;
  onCreateRoom: (x: number, y: number) => void;
  onCreateExit: (fromId: string, toId: string) => void;
}

const CELL_SIZE = 80;
const GAP = 20;
const STRIDE = CELL_SIZE + GAP; // 100
const ROOM_RADIUS = 8;

function MapCanvas({
  rooms,
  exits,
  selectedRoomId,
  onSelectRoom,
  onCreateRoom,
  onCreateExit,
}: MapCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [canvasSize, setCanvasSize] = useState({ w: 800, h: 600 });

  // Track current mouse position for drag preview
  const [dragPreview, setDragPreview] = useState<{
    fromId: string;
    mouseX: number;
    mouseY: number;
  } | null>(null);

  const dragState = useRef<{
    type: 'none' | 'pan' | 'exit';
    startX: number;
    startY: number;
    offsetStart: { x: number; y: number };
    fromRoomId?: string;
  }>({ type: 'none', startX: 0, startY: 0, offsetStart: { x: 0, y: 0 } });

  // ResizeObserver
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const ro = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        setCanvasSize({ w: Math.floor(width), h: Math.floor(height) });
      }
    });
    ro.observe(container);
    return () => ro.disconnect();
  }, []);

  // Center view initially — put grid (0,0) room center at canvas center
  const initializedRef = useRef(false);
  useEffect(() => {
    if (!initializedRef.current && canvasSize.w > 0 && canvasSize.h > 0) {
      initializedRef.current = true;
      setOffset({
        x: Math.floor(canvasSize.w / 2 - STRIDE / 2),
        y: Math.floor(canvasSize.h / 2 - STRIDE / 2),
      });
    }
  }, [canvasSize.w, canvasSize.h]);

  // Convert grid coords to pixel coords (center of cell, centered within stride)
  const gridToPixel = useCallback(
    (gx: number, gy: number) => {
      return {
        px: offset.x + gx * STRIDE + STRIDE / 2,
        py: offset.y + gy * STRIDE + STRIDE / 2,
      };
    },
    [offset]
  );

  // Convert pixel to grid coords
  const pixelToGrid = useCallback(
    (px: number, py: number) => {
      const gx = Math.floor((px - offset.x) / STRIDE);
      const gy = Math.floor((py - offset.y) / STRIDE);
      return { gx, gy };
    },
    [offset]
  );

  // Find room at pixel coords
  const roomAtPixel = useCallback(
    (px: number, py: number): RoomNode | null => {
      for (const room of rooms) {
        const { px: cx, py: cy } = gridToPixel(room.x, room.y);
        if (
          px >= cx - CELL_SIZE / 2 &&
          px <= cx + CELL_SIZE / 2 &&
          py >= cy - CELL_SIZE / 2 &&
          py <= cy + CELL_SIZE / 2
        ) {
          return room;
        }
      }
      return null;
    },
    [rooms, gridToPixel]
  );

  // Draw
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    canvas.width = canvasSize.w * dpr;
    canvas.height = canvasSize.h * dpr;
    ctx.scale(dpr, dpr);

    // Clear
    ctx.fillStyle = '#fafafa';
    ctx.fillRect(0, 0, canvasSize.w, canvasSize.h);

    // Draw grid lines
    ctx.strokeStyle = '#e8e8e8';
    ctx.lineWidth = 1;

    const startGX = Math.floor(-offset.x / STRIDE) - 1;
    const endGX = startGX + Math.ceil(canvasSize.w / STRIDE) + 2;
    const startGY = Math.floor(-offset.y / STRIDE) - 1;
    const endGY = startGY + Math.ceil(canvasSize.h / STRIDE) + 2;

    for (let gx = startGX; gx <= endGX; gx++) {
      const px = offset.x + gx * STRIDE;
      ctx.beginPath();
      ctx.moveTo(px, 0);
      ctx.lineTo(px, canvasSize.h);
      ctx.stroke();
    }
    for (let gy = startGY; gy <= endGY; gy++) {
      const py = offset.y + gy * STRIDE;
      ctx.beginPath();
      ctx.moveTo(0, py);
      ctx.lineTo(canvasSize.w, py);
      ctx.stroke();
    }

    // Build room lookup
    const roomMap = new Map<string, RoomNode>();
    for (const r of rooms) roomMap.set(r.id, r);

    // Draw exits as lines
    ctx.strokeStyle = '#888';
    ctx.lineWidth = 2;
    for (const exit of exits) {
      const from = roomMap.get(exit.fromRoomId);
      const to = roomMap.get(exit.toRoomId);
      if (!from || !to) continue;
      const { px: x1, py: y1 } = gridToPixel(from.x, from.y);
      const { px: x2, py: y2 } = gridToPixel(to.x, to.y);
      ctx.beginPath();
      ctx.moveTo(x1, y1);
      ctx.lineTo(x2, y2);
      ctx.stroke();

      // Arrowhead
      const angle = Math.atan2(y2 - y1, x2 - x1);
      const headLen = 10;
      const endX = x2 - Math.cos(angle) * (CELL_SIZE / 2);
      const endY = y2 - Math.sin(angle) * (CELL_SIZE / 2);
      ctx.beginPath();
      ctx.moveTo(endX, endY);
      ctx.lineTo(
        endX - headLen * Math.cos(angle - Math.PI / 6),
        endY - headLen * Math.sin(angle - Math.PI / 6)
      );
      ctx.moveTo(endX, endY);
      ctx.lineTo(
        endX - headLen * Math.cos(angle + Math.PI / 6),
        endY - headLen * Math.sin(angle + Math.PI / 6)
      );
      ctx.stroke();
    }

    // Draw drag preview line
    if (dragPreview) {
      const fromRoom = roomMap.get(dragPreview.fromId);
      if (fromRoom) {
        const { px: fx, py: fy } = gridToPixel(fromRoom.x, fromRoom.y);
        ctx.strokeStyle = '#ffc107';
        ctx.lineWidth = 2;
        ctx.setLineDash([6, 4]);
        ctx.beginPath();
        ctx.moveTo(fx, fy);
        ctx.lineTo(dragPreview.mouseX, dragPreview.mouseY);
        ctx.stroke();
        ctx.setLineDash([]);

        // Highlight target room if hovering over one
        const target = roomAtPixel(dragPreview.mouseX, dragPreview.mouseY);
        if (target && target.id !== dragPreview.fromId) {
          const { px: tx, py: ty } = gridToPixel(target.x, target.y);
          ctx.strokeStyle = '#4caf50';
          ctx.lineWidth = 3;
          ctx.beginPath();
          const rx = tx - CELL_SIZE / 2 - 2;
          const ry = ty - CELL_SIZE / 2 - 2;
          const rw = CELL_SIZE + 4;
          const rh = CELL_SIZE + 4;
          ctx.roundRect(rx, ry, rw, rh, ROOM_RADIUS + 2);
          ctx.stroke();
        }
      }
    }

    // Draw rooms
    for (const room of rooms) {
      const { px, py } = gridToPixel(room.x, room.y);
      const x = px - CELL_SIZE / 2;
      const y = py - CELL_SIZE / 2;

      ctx.beginPath();
      ctx.roundRect(x, y, CELL_SIZE, CELL_SIZE, ROOM_RADIUS);
      ctx.closePath();

      ctx.fillStyle = '#3f51b5';
      ctx.fill();

      if (room.id === selectedRoomId) {
        ctx.strokeStyle = '#ffc107';
        ctx.lineWidth = 3;
        ctx.stroke();
      }

      // Room name
      ctx.fillStyle = '#fff';
      ctx.font = '11px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      const displayName =
        room.name.length > 10 ? room.name.substring(0, 9) + '...' : room.name;
      ctx.fillText(displayName, px, py);
    }
  }, [rooms, exits, selectedRoomId, offset, canvasSize, gridToPixel, dragPreview, roomAtPixel]);

  // Mouse handlers
  const handleMouseDown = useCallback(
    (e: React.MouseEvent<HTMLCanvasElement>) => {
      const rect = canvasRef.current!.getBoundingClientRect();
      const px = e.clientX - rect.left;
      const py = e.clientY - rect.top;

      const room = roomAtPixel(px, py);

      if (e.shiftKey || e.button === 1) {
        dragState.current = {
          type: 'pan',
          startX: e.clientX,
          startY: e.clientY,
          offsetStart: { ...offset },
        };
        return;
      }

      if (room) {
        dragState.current = {
          type: 'exit',
          startX: px,
          startY: py,
          offsetStart: offset,
          fromRoomId: room.id,
        };
      } else {
        dragState.current = {
          type: 'pan',
          startX: e.clientX,
          startY: e.clientY,
          offsetStart: { ...offset },
        };
      }
    },
    [offset, roomAtPixel]
  );

  const handleMouseMove = useCallback(
    (e: React.MouseEvent<HTMLCanvasElement>) => {
      if (dragState.current.type === 'pan') {
        const dx = e.clientX - dragState.current.startX;
        const dy = e.clientY - dragState.current.startY;
        setOffset({
          x: dragState.current.offsetStart.x + dx,
          y: dragState.current.offsetStart.y + dy,
        });
      } else if (dragState.current.type === 'exit' && dragState.current.fromRoomId) {
        const rect = canvasRef.current!.getBoundingClientRect();
        const px = e.clientX - rect.left;
        const py = e.clientY - rect.top;
        const dx = Math.abs(px - dragState.current.startX);
        const dy = Math.abs(py - dragState.current.startY);
        // Only show preview after a small movement threshold
        if (dx > 5 || dy > 5) {
          setDragPreview({
            fromId: dragState.current.fromRoomId,
            mouseX: px,
            mouseY: py,
          });
        }
      }
    },
    []
  );

  const handleMouseUp = useCallback(
    (e: React.MouseEvent<HTMLCanvasElement>) => {
      const rect = canvasRef.current!.getBoundingClientRect();
      const px = e.clientX - rect.left;
      const py = e.clientY - rect.top;
      const state = dragState.current;
      dragState.current = { type: 'none', startX: 0, startY: 0, offsetStart: { x: 0, y: 0 } };
      setDragPreview(null);

      if (state.type === 'pan') {
        const dx = Math.abs(e.clientX - state.startX);
        const dy = Math.abs(e.clientY - state.startY);
        if (dx < 3 && dy < 3) {
          const room = roomAtPixel(px, py);
          if (!room) {
            const { gx, gy } = pixelToGrid(px, py);
            const occupied = rooms.some((r) => r.x === gx && r.y === gy);
            if (!occupied) {
              onCreateRoom(gx, gy);
            }
          }
        }
        return;
      }

      if (state.type === 'exit' && state.fromRoomId) {
        const dx = Math.abs(px - state.startX);
        const dy = Math.abs(py - state.startY);
        const targetRoom = roomAtPixel(px, py);

        if (dx < 3 && dy < 3) {
          onSelectRoom(state.fromRoomId);
        } else if (targetRoom && targetRoom.id !== state.fromRoomId) {
          onCreateExit(state.fromRoomId, targetRoom.id);
        }
      }
    },
    [roomAtPixel, pixelToGrid, rooms, onCreateRoom, onSelectRoom, onCreateExit]
  );

  return (
    <div
      ref={containerRef}
      style={{ width: '100%', height: '100%', overflow: 'hidden', cursor: 'crosshair' }}
    >
      <canvas
        ref={canvasRef}
        width={canvasSize.w}
        height={canvasSize.h}
        style={{ width: canvasSize.w, height: canvasSize.h, display: 'block' }}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onContextMenu={(e) => e.preventDefault()}
      />
    </div>
  );
}

export default MapCanvas;
