import React from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import StatusBar from './StatusBar';
import StatusBarLayout from './StatusBarLayout';
import { ILandscape, IItem, IGroup, IAssessment } from '../../../interfaces';

const items: IItem[] = [
  {
    contact: 'marvin',
    owner: 'daniel',
    fullyQualifiedIdentifier: 'fullTestIdentifier',
    identifier: 'testIdentifier',
    name: 'testIdentifier',
    description: 'testDescription',
    relations: {},
    labels: {},
    tags: [],
    type: 'service',
    icon: '',
  },
];

const groups: IGroup[] = [
  {
    fullyQualifiedIdentifier: 'groupIdentifier',
    name: 'groupName',
    items: items,
    identifier: 'groupIdentifier',
  },
];

const landscape: ILandscape = {
  name: 'landscapeTestName',
  identifier: 'testIdentifier',
  description: 'testIdentifier',
  groups: groups,
  lastUpdate: 'gestern',
  contact: 'marvin',
  owner: 'daniel',
  fullyQualifiedIdentifier: 'fullTestIdentifier',
};
const assessments: IAssessment = {
  date: '',
  results: {
    fullTestIdentifier: [
      {
        field: 'foo',
        status: 'yellow',
        message: 'bar',
        summary: true,
      },
    ],
    groupIdentifier: [
      {
        field: 'foo',
        status: 'yellow',
        message: 'bar',
        maxField: 'foo',
        summary: true,
      },
    ],
  },
};


it('should display all group information', () => {
  const { getByText } = render(
    <MemoryRouter>
      <StatusBarLayout
        onItemClick={() => {}}
        onGroupClick={() => {}}
      />
    </MemoryRouter>
  );
  expect(getByText('Assessments')).toBeInTheDocument();
});
