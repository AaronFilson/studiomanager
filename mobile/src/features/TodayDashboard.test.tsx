import { fireEvent, screen } from '@testing-library/react-native';

import { renderWithQueries, stubApi } from '../test/api';
import { TodayDashboard } from './TodayDashboard';

describe('TodayDashboard', () => {
  it('shows handover alerts including uncovered shifts and bookings, and supports tap-through links', async () => {
    stubApi({
      'GET /api/v1/studios/:studioId/today': {
        date: '2026-11-20',
        kilns: [
          {
            id: 'k1',
            name: 'Skutt 1027',
            kind: 'ELECTRIC',
            totalFirings: 42,
            available: true,
            version: 1,
          },
          {
            id: 'k2',
            name: 'Bailey Gas',
            kind: 'GAS',
            totalFirings: 12,
            available: false,
            downReason: 'Thermocouple replaced tomorrow',
            version: 2,
          },
        ],
        openFirings: [
          {
            id: 'f1',
            kilnId: 'k1',
            kilnName: 'Skutt 1027',
            kind: 'BISQUE',
            status: 'FIRING',
            createdAt: '2026-11-20T09:00:00Z',
            pieceCount: 18,
            version: 1,
            pieces: [],
          },
        ],
        waitingForBisque: 14,
        waitingForGlaze: 7,
        readyForPickup: 5,
        pastHold: 2,
        unclaimed: 1,
        maintenanceDue: [],
        outOfService: [
          {
            id: 'k2',
            name: 'Bailey Gas',
            kind: 'GAS',
            totalFirings: 12,
            available: false,
            downReason: 'Thermocouple replaced tomorrow',
            version: 2,
          },
        ],
        bookings: [
          {
            id: 'b1',
            resourceId: 'r1',
            resourceName: 'Wheel room',
            kind: 'CLASS',
            title: 'Intro to throwing',
            startsAt: '2026-11-20T18:00:00Z',
            endsAt: '2026-11-20T20:00:00Z',
            accountName: 'Ivy Instructor',
            mine: false,
          },
        ],
        shifts: [
          {
            id: 'sh1',
            title: 'Evening monitor',
            startsAt: '2026-11-20T17:00:00Z',
            endsAt: '2026-11-20T21:00:00Z',
            open: true,
            offered: false,
            minutes: 240,
            version: 0,
          },
        ],
        uncoveredShifts: [
          {
            id: 'sh1',
            title: 'Evening monitor',
            startsAt: '2026-11-20T17:00:00Z',
            endsAt: '2026-11-20T21:00:00Z',
            open: true,
            offered: false,
            minutes: 240,
            version: 0,
          },
        ],
        lowStock: [
          {
            id: 'sup1',
            name: 'B-Mix 5',
            category: 'CLAY',
            unit: '25 lb bag',
            onHand: '2',
            onOrder: '0',
            low: true,
            needsOrdering: true,
            version: 0,
          },
        ],
        openPurchaseRequests: 1,
        openProblems: [
          {
            id: 'pr1',
            assetName: 'Wheel 3',
            description: 'Pedal squeaks',
            reportedBy: 'm1',
            reportedByName: 'Mo Maker',
            reportedAt: '2026-11-20T11:00:00Z',
            open: true,
            version: 0,
          },
        ],
      },
    });
    const onOpenKiln = jest.fn();
    const onOpenKilns = jest.fn();
    const onOpenPieces = jest.fn();
    const onOpenSupplies = jest.fn();

    await renderWithQueries(
      <TodayDashboard
        studioId="s1"
        onOpenKiln={onOpenKiln}
        onOpenKilns={onOpenKilns}
        onOpenPieces={onOpenPieces}
        onOpenSupplies={onOpenSupplies}
      />,
    );

    expect(await screen.findByText(/1 shift in the next week needs cover/)).toBeOnTheScreen();
    expect(screen.getByText(/Bailey Gas is out of service: Thermocouple replaced tomorrow/)).toBeOnTheScreen();
    expect(screen.getByText(/B-Mix 5 is low \(2 left\) and not on order/)).toBeOnTheScreen();
    expect(screen.getByText(/Wheel room: Intro to throwing \(Ivy Instructor\)/)).toBeOnTheScreen();
    expect(screen.getByText(/Waiting: 14 for bisque, 7 for glaze\. Ready for pickup: 5\./)).toBeOnTheScreen();

    await fireEvent.press(screen.getByRole('button', { name: 'Skutt 1027: firing bisque, 18 pieces' }));
    expect(onOpenKiln).toHaveBeenCalledWith('k1');

    await fireEvent.press(screen.getByRole('button', { name: 'Supplies' }));
    expect(onOpenSupplies).toHaveBeenCalledTimes(1);

    await fireEvent.press(screen.getByRole('button', { name: 'Pieces' }));
    expect(onOpenPieces).toHaveBeenCalledTimes(1);

    await fireEvent.press(screen.getByRole('button', { name: 'Kilns' }));
    expect(onOpenKilns).toHaveBeenCalledTimes(1);
  });
});
