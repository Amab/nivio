import React from 'react';
import { render } from '@testing-library/react';
import OverviewLayout from './OverviewLayout';
import { MemoryRouter } from 'react-router-dom';
import { ILandscape, IItem, IGroup } from '../../../interfaces';
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

const landscapes: ILandscape = {
    name: 'landscapeTestName',
    identifier: 'testIdentifier',
    description: 'testIdentifier',
    groups: groups,
    contact: 'marvin',
    owner: 'daniel',
    fullyQualifiedIdentifier: 'fullTestIdentifier',
};
it('should render LandscapeOverview component', () => {
    const landscapesCount = 2;

    const { getByText } = render(
        <MemoryRouter>
            <OverviewLayout landscapes={[landscapes]} setSidebarContent={() => { }} landscapesCount={landscapesCount} />
        </MemoryRouter>
    );
    expect(getByText('landscapeTestName')).toBeInTheDocument();
});



it('should render LandscapeOverview component', () => {
    const landscapesCount = 1;

    const { getByText } = render(
        <MemoryRouter>
            <OverviewLayout landscapes={[landscapes]} setSidebarContent={() => { }} landscapesCount={landscapesCount} />
        </MemoryRouter>
    );
    expect(getByText('Loading landscapes...')).toBeInTheDocument();
});
