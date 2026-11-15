import { TestBed } from '@angular/core/testing';
import { Tabs } from './tabs';

function render(active = 'one') {
  const fixture = TestBed.createComponent(Tabs);
  fixture.componentRef.setInput('tabs', [
    { id: 'one', label: 'One' },
    { id: 'two', label: 'Two' },
    { id: 'three', label: 'Three' },
  ]);
  fixture.componentRef.setInput('active', active);
  fixture.detectChanges();
  const changes: string[] = [];
  fixture.componentInstance.activeChange.subscribe((id) => changes.push(id));
  const buttons = Array.from(
    (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('[role=tab]'),
  );
  return { fixture, buttons, changes };
}

describe('Tabs', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('keeps only the active tab in the Tab order and links each tab to its panel', () => {
    const { buttons } = render('two');
    expect(buttons.map((b) => b.tabIndex)).toEqual([-1, 0, -1]);
    expect(buttons[1].getAttribute('aria-controls')).toBe('tab-two-panel');
  });

  it('moves with the arrow keys, wrapping around, and with Home and End', () => {
    const { buttons, changes } = render('one');
    buttons[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft' }));
    buttons[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
    buttons[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'End' }));
    expect(changes).toEqual(['three', 'two', 'three']);
  });

  it('does not switch when the open tab says to stay', () => {
    const { fixture, buttons, changes } = render('one');
    fixture.componentRef.setInput('canLeave', () => false);
    fixture.detectChanges();
    buttons[1].click();
    expect(changes).toEqual([]);
  });
});
