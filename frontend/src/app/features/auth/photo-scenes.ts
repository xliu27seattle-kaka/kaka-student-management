/** One photograph of the entry page. All are in the public domain; see public/entry/photo-credits.json. */
export interface PhotoScene {
  src: string;
  /** CSS object-position that keeps the subject in frame when the photograph is cropped. */
  position: string;
  title: string;
  credit: string;
}

export const PHOTO_SCENES: readonly PhotoScene[] = [
  {
    src: 'entry/great-falls.jpg',
    position: '50% 58%',
    title: 'Great Falls of the Potomac, Maryland',
    credit: 'National Park Service',
  },
  {
    src: 'entry/tidal-basin.jpg',
    position: '50% 40%',
    title: 'Cherry blossoms at the Tidal Basin, Washington DC',
    credit: 'Carol M. Highsmith, Library of Congress',
  },
  {
    src: 'entry/assateague.jpg',
    position: '42% 50%',
    title: 'Assateague Island, Maryland',
    credit: 'National Park Service',
  },
  {
    src: 'entry/catoctin.jpg',
    position: '50% 46%',
    title: 'Hog Rock Overlook, Catoctin Mountain Park, Maryland',
    credit: 'National Park Service',
  },
  {
    src: 'entry/canal-towpath.jpg',
    position: '50% 50%',
    title: 'C&O Canal towpath, Maryland',
    credit: 'Bonnachoven, CC0',
  },
];
